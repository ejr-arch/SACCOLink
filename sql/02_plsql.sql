-- ============================================================================
-- SACCOLink (Simplified Prototype)
-- PL/SQL: Scoring engine, passport generation, passport verification
-- ============================================================================

-- ----------------------------------------------------------------------------
-- FN_GET_SCORE_BAND : map numeric score (0-850) to a band label
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_GET_SCORE_BAND (p_score IN NUMBER)
RETURN VARCHAR2
IS
BEGIN
    IF p_score >= 700 THEN RETURN 'EXCELLENT';
    ELSIF p_score >= 550 THEN RETURN 'GOOD';
    ELSIF p_score >= 400 THEN RETURN 'FAIR';
    ELSE RETURN 'THIN';
    END IF;
END FN_GET_SCORE_BAND;
/

-- ----------------------------------------------------------------------------
-- SP_COMPUTE_SCORE : compute and store a new score for a member.
--   - Loan Repayment (60%) : repaid/total loans (0-100), -25 per default
--   - Savings Consistency (40%) : months contributed / months since first (0-100)
--   - Composite = (REPAYMENT*0.6 + SAVINGS*0.4) * 8.5  -> 0-850
--   - Marks the previous current score IS_CURRENT = 'N'
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_COMPUTE_SCORE (p_member_id IN NUMBER)
IS
    v_total_loans   NUMBER := 0;
    v_repaid_loans  NUMBER := 0;
    v_default_loans NUMBER := 0;
    v_repay_score   NUMBER := 0;
    v_savings_score NUMBER := 0;
    v_months_data   NUMBER := 0;
    v_months_since  NUMBER := 0;
    v_first_month   DATE;
    v_composite     NUMBER;
    v_band          VARCHAR2(20);
    v_no_member     EXCEPTION;
    PRAGMA EXCEPTION_INIT(v_no_member, -20001);
BEGIN
    -- member must exist
    SELECT COUNT(*) INTO v_total_loans
      FROM MEMBER WHERE MEMBER_ID = p_member_id;
    IF v_total_loans = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'MEMBER_NOT_FOUND');
    END IF;

    -- --- Loan repayment factor (0-100) ---
    SELECT COUNT(*), NVL(SUM(CASE WHEN LOAN_STATUS = 'REPAID'    THEN 1 ELSE 0 END), 0),
           NVL(SUM(CASE WHEN LOAN_STATUS = 'DEFAULTED' THEN 1 ELSE 0 END), 0)
      INTO v_total_loans, v_repaid_loans, v_default_loans
      FROM LOAN_RECORD
     WHERE MEMBER_ID = p_member_id;

    IF v_total_loans > 0 THEN
        v_repay_score := (v_repaid_loans / v_total_loans) * 100
                         - (v_default_loans * 25);
        v_repay_score := GREATEST(v_repay_score, 0);
    ELSE
        v_repay_score := 50; -- neutral when no loan history
    END IF;

    -- --- Savings consistency factor (0-100) ---
    SELECT COUNT(*), MIN(CONTRIBUTION_MONTH)
      INTO v_months_data, v_first_month
      FROM SAVINGS_RECORD
     WHERE MEMBER_ID = p_member_id;

    IF v_months_data > 0 THEN
        v_months_since := MONTHS_BETWEEN(TRUNC(SYSDATE, 'MM'), v_first_month) + 1;
        v_months_since := GREATEST(v_months_since, 1);
        v_savings_score := (v_months_data / v_months_since) * 100;
        v_savings_score := LEAST(v_savings_score, 100);
    ELSE
        v_savings_score := 0;
    END IF;

    -- --- Composite score (0-850) ---
    v_composite := ROUND((v_repay_score * 0.6 + v_savings_score * 0.4) * 8.5, 2);
    v_composite := LEAST(v_composite, 850);
    v_band := FN_GET_SCORE_BAND(v_composite);

    -- previous score no longer current
    UPDATE CREDIT_SCORE SET IS_CURRENT = 'N'
     WHERE MEMBER_ID = p_member_id AND IS_CURRENT = 'Y';

    INSERT INTO CREDIT_SCORE (MEMBER_ID, SCORE_VALUE, SCORE_BAND,
                              REPAYMENT_SCORE, SAVINGS_SCORE)
    VALUES (p_member_id, v_composite, v_band, v_repay_score, v_savings_score);

    DBMS_OUTPUT.PUT_LINE('Score ' || v_composite || ' (' || v_band ||
                         ') computed for member ' || p_member_id);
END SP_COMPUTE_SCORE;
/

-- ----------------------------------------------------------------------------
-- SP_GENERATE_PASSPORT : create a 72-hour credit passport for a member.
--   Requires consent and a current score. QR token is a random 32-char string.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_GENERATE_PASSPORT (
    p_member_id    IN  NUMBER,
    p_passport_id  OUT NUMBER,
    p_qr_token     OUT VARCHAR2
)
IS
    v_score_id  NUMBER;
    v_token     VARCHAR2(64);
    v_consent   CHAR(1);
BEGIN
    -- consent gate
    BEGIN
        SELECT CONSENT_GIVEN INTO v_consent
          FROM MEMBER WHERE MEMBER_ID = p_member_id;
        IF v_consent != 'Y' THEN
            RAISE_APPLICATION_ERROR(-20002, 'CONSENT_NOT_GIVEN');
        END IF;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20001, 'MEMBER_NOT_FOUND');
    END;

    -- current score required
    SELECT SCORE_ID INTO v_score_id
      FROM CREDIT_SCORE
     WHERE MEMBER_ID = p_member_id AND IS_CURRENT = 'Y';

    v_token := DBMS_RANDOM.STRING('X', 32);

    INSERT INTO CREDIT_PASSPORT (MEMBER_ID, SCORE_ID, QR_TOKEN, EXPIRES_AT)
    VALUES (p_member_id, v_score_id, v_token, SYSTIMESTAMP + INTERVAL '3' DAY)
    RETURNING PASSPORT_ID INTO p_passport_id;

    p_qr_token := v_token;
END SP_GENERATE_PASSPORT;
/

-- ----------------------------------------------------------------------------
-- FN_VERIFY_PASSPORT : verify a QR token.
--   Returns 'VALID' / 'EXPIRED' / 'REVOKED' / 'NOT_FOUND'.
--   On VALID, increments the passport VIEW_COUNT.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_VERIFY_PASSPORT (
    p_qr_token      IN  VARCHAR2,
    p_member_name   OUT VARCHAR2,
    p_score_value   OUT NUMBER,
    p_score_band    OUT VARCHAR2,
    p_generated_at  OUT TIMESTAMP,
    p_expires_at    OUT TIMESTAMP
)
RETURN VARCHAR2
IS
    v_status    VARCHAR2(20);
    v_expires   TIMESTAMP;
BEGIN
    SELECT PASSPORT_STATUS, EXPIRES_AT
      INTO v_status, v_expires
      FROM CREDIT_PASSPORT
     WHERE QR_TOKEN = p_qr_token;

    IF v_status = 'REVOKED' THEN
        RETURN 'REVOKED';
    END IF;

    IF v_status = 'ACTIVE' AND v_expires > SYSTIMESTAMP THEN
        SELECT m.FULL_NAME, s.SCORE_VALUE, s.SCORE_BAND,
               p.GENERATED_AT, p.EXPIRES_AT
          INTO p_member_name, p_score_value, p_score_band,
               p_generated_at, p_expires_at
          FROM CREDIT_PASSPORT p
          JOIN MEMBER m       ON m.MEMBER_ID = p.MEMBER_ID
          JOIN CREDIT_SCORE s ON s.SCORE_ID  = p.SCORE_ID
         WHERE p.QR_TOKEN = p_qr_token;

        UPDATE CREDIT_PASSPORT SET VIEW_COUNT = VIEW_COUNT + 1
         WHERE QR_TOKEN = p_qr_token;
        RETURN 'VALID';
    END IF;

    RETURN 'EXPIRED';
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 'NOT_FOUND';
END FN_VERIFY_PASSPORT;
/

-- ----------------------------------------------------------------------------
-- SP_EXPIRE_PASSPORTS : mark passports whose EXPIRES_AT has passed.
--   Run manually or via a scheduled DBMS_SCHEDULER job.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_EXPIRE_PASSPORTS
IS
BEGIN
    UPDATE CREDIT_PASSPORT
       SET PASSPORT_STATUS = 'EXPIRED'
     WHERE PASSPORT_STATUS = 'ACTIVE'
       AND EXPIRES_AT < SYSTIMESTAMP;
    COMMIT;
END SP_EXPIRE_PASSPORTS;
/

PROMPT SACCOLink PL/SQL objects created successfully.

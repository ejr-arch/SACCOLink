-- ============================================================================
-- SACCOLink (Simplified Prototype)
-- Packages -- single entry points wrapping the PL/SQL business logic
-- Run AFTER sql/02_plsql.sql (standalone objects) and sql/04_triggers.sql.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PKG_SACCOINK : public API over scoring + passport workflow.
-- Thin wrappers keep the standalone routines callable by the Java Swing client
-- over JDBC and provide a single versioned entry point for applications.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PACKAGE PKG_SACCOINK AS

    FUNCTION GET_SCORE_BAND(p_score IN NUMBER)
        RETURN VARCHAR2;

    PROCEDURE COMPUTE_SCORE(p_member_id IN NUMBER);

    PROCEDURE GENERATE_PASSPORT(
        p_member_id   IN  NUMBER,
        p_passport_id OUT NUMBER,
        p_qr_token    OUT VARCHAR2);

    FUNCTION VERIFY_PASSPORT(
        p_qr_token     IN  VARCHAR2,
        p_member_name  OUT VARCHAR2,
        p_score_value  OUT NUMBER,
        p_score_band   OUT VARCHAR2,
        p_generated_at OUT TIMESTAMP,
        p_expires_at   OUT TIMESTAMP)
        RETURN VARCHAR2;

    PROCEDURE EXPIRE_PASSPORTS;

    -- Total still owed on a member's ACTIVE loans, computed with an EXPLICIT
    -- CURSOR (OPEN / FETCH / %ROWTYPE / CLOSE) for the PL/SQL coursework.
    FUNCTION OUTSTANDING_BALANCE(p_member_id IN NUMBER) RETURN NUMBER;

END PKG_SACCOINK;
/

CREATE OR REPLACE PACKAGE BODY PKG_SACCOINK AS

    FUNCTION GET_SCORE_BAND(p_score IN NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN FN_GET_SCORE_BAND(p_score);
    END GET_SCORE_BAND;

    PROCEDURE COMPUTE_SCORE(p_member_id IN NUMBER) IS
    BEGIN
        SP_COMPUTE_SCORE(p_member_id);
    END COMPUTE_SCORE;

    PROCEDURE GENERATE_PASSPORT(
        p_member_id   IN  NUMBER,
        p_passport_id OUT NUMBER,
        p_qr_token    OUT VARCHAR2) IS
    BEGIN
        SP_GENERATE_PASSPORT(p_member_id, p_passport_id, p_qr_token);
    END GENERATE_PASSPORT;

    FUNCTION VERIFY_PASSPORT(
        p_qr_token     IN  VARCHAR2,
        p_member_name  OUT VARCHAR2,
        p_score_value  OUT NUMBER,
        p_score_band   OUT VARCHAR2,
        p_generated_at OUT TIMESTAMP,
        p_expires_at   OUT TIMESTAMP) RETURN VARCHAR2 IS
    BEGIN
        RETURN FN_VERIFY_PASSPORT(p_qr_token, p_member_name, p_score_value,
                                  p_score_band, p_generated_at, p_expires_at);
    END VERIFY_PASSPORT;

    PROCEDURE EXPIRE_PASSPORTS IS
    BEGIN
        SP_EXPIRE_PASSPORTS;
    END EXPIRE_PASSPORTS;

    FUNCTION OUTSTANDING_BALANCE(p_member_id IN NUMBER) RETURN NUMBER IS
        CURSOR c_outstanding IS
            SELECT *
              FROM LOAN_RECORD
             WHERE MEMBER_ID = p_member_id
               AND LOAN_STATUS = 'ACTIVE';
        v_balance NUMBER := 0;
        v_row     LOAN_RECORD%ROWTYPE;
    BEGIN
        OPEN c_outstanding;
        LOOP
            FETCH c_outstanding INTO v_row;
            EXIT WHEN c_outstanding%NOTFOUND;
            v_balance := v_balance + v_row.LOAN_AMOUNT;
        END LOOP;
        CLOSE c_outstanding;
        RETURN v_balance;
    EXCEPTION
        WHEN OTHERS THEN
            IF c_outstanding%ISOPEN THEN
                CLOSE c_outstanding;
            END IF;
            RAISE;
    END OUTSTANDING_BALANCE;

END PKG_SACCOINK;
/

-- ----------------------------------------------------------------------------
-- PKG_SACCOINK_REPORT : aggregate statistics for dashboards and reports.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PACKAGE PKG_SACCOINK_REPORT AS

    FUNCTION COUNT_MEMBERS RETURN NUMBER;
    FUNCTION COUNT_MEMBERS_WITH_CONSENT RETURN NUMBER;
    FUNCTION AVG_CURRENT_SCORE RETURN NUMBER;
    FUNCTION COUNT_PASSPORTS(p_status IN VARCHAR2 DEFAULT NULL) RETURN NUMBER;
    FUNCTION TOTAL_SAVINGS(p_member_id IN NUMBER DEFAULT NULL) RETURN NUMBER;
    FUNCTION TOTAL_LOANS(p_member_id IN NUMBER DEFAULT NULL) RETURN NUMBER;

    PROCEDURE MEMBER_SUMMARY(
        p_member_id IN  NUMBER,
        p_loans     OUT NUMBER,
        p_savings   OUT NUMBER,
        p_score     OUT NUMBER,
        p_band      OUT VARCHAR2);

END PKG_SACCOINK_REPORT;
/

CREATE OR REPLACE PACKAGE BODY PKG_SACCOINK_REPORT AS

    FUNCTION COUNT_MEMBERS RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM MEMBER;
        RETURN v_count;
    END COUNT_MEMBERS;

    FUNCTION COUNT_MEMBERS_WITH_CONSENT RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM MEMBER WHERE CONSENT_GIVEN = 'Y';
        RETURN v_count;
    END COUNT_MEMBERS_WITH_CONSENT;

    FUNCTION AVG_CURRENT_SCORE RETURN NUMBER IS
        v_avg NUMBER;
    BEGIN
        SELECT NVL(AVG(SCORE_VALUE), 0) INTO v_avg
          FROM CREDIT_SCORE WHERE IS_CURRENT = 'Y';
        RETURN v_avg;
    END AVG_CURRENT_SCORE;

    FUNCTION COUNT_PASSPORTS(p_status IN VARCHAR2 DEFAULT NULL) RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        IF p_status IS NULL THEN
            SELECT COUNT(*) INTO v_count FROM CREDIT_PASSPORT;
        ELSE
            SELECT COUNT(*) INTO v_count FROM CREDIT_PASSPORT
             WHERE PASSPORT_STATUS = p_status;
        END IF;
        RETURN v_count;
    END COUNT_PASSPORTS;

    FUNCTION TOTAL_SAVINGS(p_member_id IN NUMBER DEFAULT NULL) RETURN NUMBER IS
        v_total NUMBER;
    BEGIN
        IF p_member_id IS NULL THEN
            SELECT NVL(SUM(AMOUNT_CONTRIBUTED), 0) INTO v_total FROM SAVINGS_RECORD;
        ELSE
            SELECT NVL(SUM(AMOUNT_CONTRIBUTED), 0) INTO v_total FROM SAVINGS_RECORD
             WHERE MEMBER_ID = p_member_id;
        END IF;
        RETURN v_total;
    END TOTAL_SAVINGS;

    FUNCTION TOTAL_LOANS(p_member_id IN NUMBER DEFAULT NULL) RETURN NUMBER IS
        v_total NUMBER;
    BEGIN
        IF p_member_id IS NULL THEN
            SELECT NVL(SUM(LOAN_AMOUNT), 0) INTO v_total FROM LOAN_RECORD;
        ELSE
            SELECT NVL(SUM(LOAN_AMOUNT), 0) INTO v_total FROM LOAN_RECORD
             WHERE MEMBER_ID = p_member_id;
        END IF;
        RETURN v_total;
    END TOTAL_LOANS;

    PROCEDURE MEMBER_SUMMARY(
        p_member_id IN  NUMBER,
        p_loans     OUT NUMBER,
        p_savings   OUT NUMBER,
        p_score     OUT NUMBER,
        p_band      OUT VARCHAR2) IS
        v_member_id NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_member_id FROM MEMBER WHERE MEMBER_ID = p_member_id;
        IF v_member_id = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'MEMBER_NOT_FOUND');
        END IF;

        SELECT COUNT(*) INTO p_loans FROM LOAN_RECORD WHERE MEMBER_ID = p_member_id;
        SELECT NVL(SUM(AMOUNT_CONTRIBUTED), 0) INTO p_savings
          FROM SAVINGS_RECORD WHERE MEMBER_ID = p_member_id;

        BEGIN
            SELECT SCORE_VALUE, SCORE_BAND INTO p_score, p_band
              FROM CREDIT_SCORE
             WHERE MEMBER_ID = p_member_id AND IS_CURRENT = 'Y';
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                p_score := NULL;
                p_band  := 'NO_SCORE';
        END;
    END MEMBER_SUMMARY;

END PKG_SACCOINK_REPORT;
/

PROMPT SACCOLink packages created successfully (2 packages).

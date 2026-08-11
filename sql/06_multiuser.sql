-- ============================================================================
-- SACCOLink (Simplified Prototype)
-- Multi-user simulation: APP_USER (MEMBER / SACCO) + LOAN_REQUEST workflow
--
--   * Two account types
--       - MEMBER : sees ONLY their own history (loans, savings, scores,
--                  passports) and can generate a token for a creditor.
--       - SACCO  : full access; verifies creditworthiness when a member
--                  requests a loan, then approves / rejects the request
--                  (approval records the loan on LOAN_RECORD).
--   * V_MY_LOANS / V_MY_SAVINGS / V_MY_SCORES / V_MY_PASSPORTS / V_MY_PROFILE
--     views scope each user to their own rows using an application context
--     (saccolink_ctx) set at login. SACCO (MEMBER_ID = NULL) sees all rows.
--
-- Run AFTER 01_schema.sql, 02_plsql.sql, 04_triggers.sql, 05_packages.sql
-- and 03_sample_data.sql (needs MEMBER rows for the user seeds).
-- ============================================================================
SET SERVEROUTPUT ON

-- ----------------------------------------------------------------------------
-- Cleanup (tolerant, so 06 also runs standalone / after a fresh 01-schema reset)
-- ----------------------------------------------------------------------------
DECLARE
BEGIN
    -- The context must be dropped BEFORE its USING package, otherwise a later
    -- CREATE OR REPLACE CONTEXT leaves SET_CONTEXT raising ORA-01031.
    EXECUTE IMMEDIATE 'DROP CONTEXT saccolink_ctx';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

DECLARE
BEGIN
    FOR o IN (SELECT object_name, object_type FROM user_objects
               WHERE object_name IN ('APP_USER', 'LOAN_REQUEST',
                                     'SEQ_APP_USER', 'SEQ_LOAN_REQUEST',
                                     'V_MY_PASSPORTS', 'V_MY_SCORES',
                                     'V_MY_SAVINGS', 'V_MY_LOANS', 'V_MY_PROFILE',
                                     'PKG_APP_SESSION')
                 AND object_type IN ('TABLE', 'SEQUENCE', 'VIEW', 'PACKAGE',
                                     'PACKAGE BODY', 'PROCEDURE', 'FUNCTION')) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP ' || o.object_type || ' "' || o.object_name || '"';
        EXCEPTION
            WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END;
/

-- ----------------------------------------------------------------------------
-- Sequences (PKs stay classic NUMBER + BEFORE INSERT trigger pattern)
-- ----------------------------------------------------------------------------
CREATE SEQUENCE SEQ_APP_USER      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_LOAN_REQUEST  START WITH 1 INCREMENT BY 1 NOCACHE;

-- ----------------------------------------------------------------------------
-- APP_USER : application login (not a database account)
-- ----------------------------------------------------------------------------
CREATE TABLE APP_USER (
    USER_ID       NUMBER       NOT NULL CONSTRAINT PK_APP_USER PRIMARY KEY,
    USERNAME      VARCHAR2(50) NOT NULL CONSTRAINT UQ_APP_USER_USERNAME UNIQUE,
    PASSWORD_HASH VARCHAR2(64) NOT NULL,   -- SHA-256 hex (username#password)
    ROLE          VARCHAR2(10) NOT NULL
                  CONSTRAINT CK_APP_USER_ROLE CHECK (ROLE IN ('MEMBER','SACCO')),
    MEMBER_ID     NUMBER
                  CONSTRAINT FK_APP_USER_MEMBER REFERENCES MEMBER(MEMBER_ID),
    DISPLAY_NAME  VARCHAR2(200) NOT NULL,
    IS_ACTIVE     CHAR(1)       DEFAULT 'Y'
                  CONSTRAINT CK_APP_USER_ACTIVE CHECK (IS_ACTIVE IN ('Y','N')),
    CREATED_AT    TIMESTAMP     DEFAULT SYSTIMESTAMP
);

CREATE OR REPLACE TRIGGER TRG_APP_USER_BI
BEFORE INSERT ON APP_USER FOR EACH ROW
BEGIN
    IF :NEW.USER_ID IS NULL THEN
        SELECT SEQ_APP_USER.NEXTVAL INTO :NEW.USER_ID FROM DUAL;
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- LOAN_REQUEST : member asks for a loan; SACCO reviews it
-- ----------------------------------------------------------------------------
CREATE TABLE LOAN_REQUEST (
    REQUEST_ID       NUMBER       NOT NULL CONSTRAINT PK_LOAN_REQUEST PRIMARY KEY,
    MEMBER_ID        NUMBER       NOT NULL
                     CONSTRAINT FK_LR_MEMBER REFERENCES MEMBER(MEMBER_ID),
    REQUESTED_AMOUNT NUMBER(15,2) NOT NULL
                     CONSTRAINT CK_LR_AMOUNT CHECK (REQUESTED_AMOUNT > 0),
    PURPOSE          VARCHAR2(500),
    REQUESTED_AT     TIMESTAMP    DEFAULT SYSTIMESTAMP,
    STATUS           VARCHAR2(20) DEFAULT 'PENDING' NOT NULL
                     CONSTRAINT CK_LR_STATUS
                     CHECK (STATUS IN ('PENDING','APPROVED','REJECTED')),
    REVIEWED_BY      VARCHAR2(50),
    REVIEWED_AT      TIMESTAMP
);

CREATE INDEX IDX_LR_MEMBER ON LOAN_REQUEST(MEMBER_ID);
CREATE INDEX IDX_LR_STATUS ON LOAN_REQUEST(STATUS);

CREATE OR REPLACE TRIGGER TRG_LOAN_REQUEST_BI
BEFORE INSERT ON LOAN_REQUEST FOR EACH ROW
BEGIN
    IF :NEW.REQUEST_ID IS NULL THEN
        SELECT SEQ_LOAN_REQUEST.NEXTVAL INTO :NEW.REQUEST_ID FROM DUAL;
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- Application context + session package (drives the V_MY_* views)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PACKAGE PKG_APP_SESSION AS
    PROCEDURE SET_MEMBER(p_member_id IN NUMBER);
    PROCEDURE CLEAR;
END PKG_APP_SESSION;
/

CREATE OR REPLACE CONTEXT saccolink_ctx USING PKG_APP_SESSION;

CREATE OR REPLACE PACKAGE BODY PKG_APP_SESSION AS
    PROCEDURE SET_MEMBER(p_member_id IN NUMBER) IS
    BEGIN
        DBMS_SESSION.SET_CONTEXT('saccolink_ctx', 'MEMBER_ID',
                                 TO_CHAR(p_member_id));
    END SET_MEMBER;

    PROCEDURE CLEAR IS
    BEGIN
        DBMS_SESSION.CLEAR_CONTEXT('saccolink_ctx');
    END CLEAR;
END PKG_APP_SESSION;
/

-- ----------------------------------------------------------------------------
-- Password hashing + login
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_HASH_PASSWORD (
    p_username IN VARCHAR2,
    p_password IN VARCHAR2
) RETURN VARCHAR2 IS
    v_hash VARCHAR2(64);
BEGIN
    -- STANDARD_HASH is a SQL function only, so it is called dynamically.
    EXECUTE IMMEDIATE
        'SELECT LOWER(RAWTOHEX(STANDARD_HASH(:1, ''SHA256''))) FROM DUAL'
        INTO v_hash USING p_username || '#' || p_password;
    RETURN v_hash;
END FN_HASH_PASSWORD;
/

-- SP_LOGIN : validates an APP_USER credential and returns the session fields.
-- On success it also sets the application context for the V_MY_* views.
CREATE OR REPLACE PROCEDURE SP_LOGIN (
    p_username     IN  VARCHAR2,
    p_password     IN  VARCHAR2,
    p_user_id      OUT NUMBER,
    p_role         OUT VARCHAR2,
    p_member_id    OUT NUMBER,
    p_display_name OUT VARCHAR2,
    p_ok           OUT NUMBER
) IS
    v_member_id NUMBER;
BEGIN
    p_ok := 0;
    SELECT USER_ID, ROLE, MEMBER_ID, DISPLAY_NAME
      INTO p_user_id, p_role, v_member_id, p_display_name
      FROM APP_USER
     WHERE USERNAME = p_username
       AND PASSWORD_HASH = FN_HASH_PASSWORD(p_username, p_password)
       AND IS_ACTIVE = 'Y';

    p_member_id := v_member_id;
    p_ok := 1;
    PKG_APP_SESSION.SET_MEMBER(v_member_id);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_ok := 0;
        p_user_id      := NULL;
        p_role         := NULL;
        p_member_id    := NULL;
        p_display_name := NULL;
END SP_LOGIN;
/

-- ----------------------------------------------------------------------------
-- Loan request workflow
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_REQUEST_LOAN (
    p_member_id IN NUMBER,
    p_amount    IN NUMBER,
    p_purpose   IN VARCHAR2
) IS
BEGIN
    INSERT INTO LOAN_REQUEST (MEMBER_ID, REQUESTED_AMOUNT, PURPOSE)
    VALUES (p_member_id, p_amount, p_purpose);
    COMMIT;
END SP_REQUEST_LOAN;
/

-- SP_REVIEW_LOAN : SACCO decides on a pending request. 'APPROVED' also records
-- the loan on LOAN_RECORD as ACTIVE (disbursed today). 'REJECTED' just updates.
CREATE OR REPLACE PROCEDURE SP_REVIEW_LOAN (
    p_request_id  IN NUMBER,
    p_decision    IN VARCHAR2,
    p_reviewed_by IN VARCHAR2
) IS
    v_member_id NUMBER;
    v_amount    NUMBER;
BEGIN
    SELECT MEMBER_ID, REQUESTED_AMOUNT
      INTO v_member_id, v_amount
      FROM LOAN_REQUEST WHERE REQUEST_ID = p_request_id;

    IF UPPER(p_decision) = 'APPROVED' THEN
        INSERT INTO LOAN_RECORD (MEMBER_ID, LOAN_AMOUNT, DISBURSEMENT_DATE,
                                 REPAYMENT_DATE, LOAN_STATUS)
        VALUES (v_member_id, v_amount, TRUNC(SYSDATE), NULL, 'ACTIVE');

        UPDATE LOAN_REQUEST
           SET STATUS = 'APPROVED', REVIEWED_BY = p_reviewed_by,
               REVIEWED_AT = SYSTIMESTAMP
         WHERE REQUEST_ID = p_request_id;
    ELSIF UPPER(p_decision) = 'REJECTED' THEN
        UPDATE LOAN_REQUEST
           SET STATUS = 'REJECTED', REVIEWED_BY = p_reviewed_by,
               REVIEWED_AT = SYSTIMESTAMP
         WHERE REQUEST_ID = p_request_id;
    ELSE
        RAISE_APPLICATION_ERROR(-20010, 'INVALID_DECISION');
    END IF;
    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20011, 'REQUEST_NOT_FOUND');
END SP_REVIEW_LOAN;
/

-- ----------------------------------------------------------------------------
-- Per-user views.
-- A MEMBER only ever sees their own rows; SACCO (context MEMBER_ID = NULL)
-- sees everything. Used by the GUI DAOs and demonstrable in SQL Developer.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW V_MY_LOANS AS
SELECT l.LOAN_ID, l.MEMBER_ID, m.FULL_NAME AS MEMBER_NAME,
       l.LOAN_AMOUNT, l.DISBURSEMENT_DATE, l.REPAYMENT_DATE, l.LOAN_STATUS
  FROM LOAN_RECORD l
  JOIN MEMBER m ON m.MEMBER_ID = l.MEMBER_ID
 WHERE SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID') IS NULL
    OR l.MEMBER_ID = TO_NUMBER(SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID'));

CREATE OR REPLACE VIEW V_MY_SAVINGS AS
SELECT s.SAVINGS_ID, s.MEMBER_ID, m.FULL_NAME AS MEMBER_NAME,
       s.CONTRIBUTION_MONTH, s.AMOUNT_CONTRIBUTED
  FROM SAVINGS_RECORD s
  JOIN MEMBER m ON m.MEMBER_ID = s.MEMBER_ID
 WHERE SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID') IS NULL
    OR s.MEMBER_ID = TO_NUMBER(SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID'));

CREATE OR REPLACE VIEW V_MY_SCORES AS
SELECT s.SCORE_ID, s.MEMBER_ID, m.FULL_NAME AS MEMBER_NAME, s.SCORE_VALUE,
       s.SCORE_BAND, s.REPAYMENT_SCORE, s.SAVINGS_SCORE,
       s.COMPUTED_AT, s.IS_CURRENT
  FROM CREDIT_SCORE s
  JOIN MEMBER m ON m.MEMBER_ID = s.MEMBER_ID
 WHERE SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID') IS NULL
    OR s.MEMBER_ID = TO_NUMBER(SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID'));

CREATE OR REPLACE VIEW V_MY_PASSPORTS AS
SELECT p.PASSPORT_ID, p.QR_TOKEN, p.PASSPORT_STATUS, p.GENERATED_AT,
       p.EXPIRES_AT, p.VIEW_COUNT, m.MEMBER_ID, m.FULL_NAME, m.NIN,
       m.DISTRICT, s.SCORE_VALUE, s.SCORE_BAND
  FROM CREDIT_PASSPORT p
  JOIN MEMBER m ON m.MEMBER_ID = p.MEMBER_ID
  JOIN CREDIT_SCORE s ON s.SCORE_ID = p.SCORE_ID
 WHERE SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID') IS NULL
    OR p.MEMBER_ID = TO_NUMBER(SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID'));

CREATE OR REPLACE VIEW V_MY_PROFILE AS
SELECT MEMBER_ID, NIN, FULL_NAME, PHONE_NUMBER, DISTRICT, CONSENT_GIVEN,
       CREATED_AT
  FROM MEMBER
 WHERE SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID') IS NULL
    OR MEMBER_ID = TO_NUMBER(SYS_CONTEXT('saccolink_ctx', 'MEMBER_ID'));

-- ----------------------------------------------------------------------------
-- Seed application users
-- ----------------------------------------------------------------------------
INSERT INTO APP_USER (USERNAME, PASSWORD_HASH, ROLE, MEMBER_ID, DISPLAY_NAME)
SELECT 'sacco',  FN_HASH_PASSWORD('sacco',  'sacco123'),
       'SACCO', NULL, 'SACCO Officer' FROM DUAL;

INSERT INTO APP_USER (USERNAME, PASSWORD_HASH, ROLE, MEMBER_ID, DISPLAY_NAME)
SELECT 'grace', FN_HASH_PASSWORD('grace', 'member123'), 'MEMBER', MEMBER_ID, FULL_NAME
  FROM MEMBER WHERE FULL_NAME = 'Grace Namukasa';

INSERT INTO APP_USER (USERNAME, PASSWORD_HASH, ROLE, MEMBER_ID, DISPLAY_NAME)
SELECT 'okello', FN_HASH_PASSWORD('okello', 'member123'), 'MEMBER', MEMBER_ID, FULL_NAME
  FROM MEMBER WHERE FULL_NAME = 'Okello Moses';

INSERT INTO APP_USER (USERNAME, PASSWORD_HASH, ROLE, MEMBER_ID, DISPLAY_NAME)
SELECT 'hassan', FN_HASH_PASSWORD('hassan', 'member123'), 'MEMBER', MEMBER_ID, FULL_NAME
  FROM MEMBER WHERE FULL_NAME = 'Hassan Ssekabira';

INSERT INTO APP_USER (USERNAME, PASSWORD_HASH, ROLE, MEMBER_ID, DISPLAY_NAME)
SELECT 'amina', FN_HASH_PASSWORD('amina', 'member123'), 'MEMBER', MEMBER_ID, FULL_NAME
  FROM MEMBER WHERE FULL_NAME = 'Amina Nakato';

INSERT INTO APP_USER (USERNAME, PASSWORD_HASH, ROLE, MEMBER_ID, DISPLAY_NAME)
SELECT 'sarah', FN_HASH_PASSWORD('sarah', 'member123'), 'MEMBER', MEMBER_ID, FULL_NAME
  FROM MEMBER WHERE FULL_NAME = 'Sarah Kintu';

INSERT INTO APP_USER (USERNAME, PASSWORD_HASH, ROLE, MEMBER_ID, DISPLAY_NAME)
SELECT 'john', FN_HASH_PASSWORD('john', 'member123'), 'MEMBER', MEMBER_ID, FULL_NAME
  FROM MEMBER WHERE FULL_NAME = 'John Ochieng';

INSERT INTO APP_USER (USERNAME, PASSWORD_HASH, ROLE, MEMBER_ID, DISPLAY_NAME)
SELECT 'betty', FN_HASH_PASSWORD('betty', 'member123'), 'MEMBER', MEMBER_ID, FULL_NAME
  FROM MEMBER WHERE FULL_NAME = 'Betty Nansubuga';

-- ----------------------------------------------------------------------------
-- Seed a couple of loan requests
-- ----------------------------------------------------------------------------
INSERT INTO LOAN_REQUEST (MEMBER_ID, REQUESTED_AMOUNT, PURPOSE)
SELECT MEMBER_ID, 2500000, 'School fees'
  FROM MEMBER WHERE FULL_NAME = 'John Ochieng';

INSERT INTO LOAN_REQUEST (MEMBER_ID, REQUESTED_AMOUNT, PURPOSE)
SELECT MEMBER_ID, 1200000, 'Shop stock'
  FROM MEMBER WHERE FULL_NAME = 'Amina Nakato';

COMMIT;

-- ----------------------------------------------------------------------------
-- Demonstration run
-- ----------------------------------------------------------------------------
DECLARE
    v_ok     NUMBER;
    v_uid    NUMBER;
    v_role   VARCHAR2(20);
    v_mid    NUMBER;
    v_name   VARCHAR2(200);
    v_req_id NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- SP_LOGIN (sacco / sacco123) ---');
    SP_LOGIN('sacco', 'sacco123', v_uid, v_role, v_mid, v_name, v_ok);
    DBMS_OUTPUT.PUT_LINE('ok=' || v_ok || ' user_id=' || v_uid ||
                         ' role=' || v_role || ' member_id=' || NVL(TO_CHAR(v_mid),'NULL') ||
                         ' name=' || v_name);

    DBMS_OUTPUT.PUT_LINE('--- SP_LOGIN (wrong password) ---');
    SP_LOGIN('sacco', 'wrong', v_uid, v_role, v_mid, v_name, v_ok);
    DBMS_OUTPUT.PUT_LINE('ok=' || v_ok);

    DBMS_OUTPUT.PUT_LINE('--- SP_LOGIN (grace / member123) ---');
    SP_LOGIN('grace', 'member123', v_uid, v_role, v_mid, v_name, v_ok);
    DBMS_OUTPUT.PUT_LINE('ok=' || v_ok || ' role=' || v_role ||
                         ' member_id=' || v_mid || ' name=' || v_name);

    DBMS_OUTPUT.PUT_LINE('--- Grace requests a 1,000,000 loan ---');
    SP_REQUEST_LOAN(v_mid, 1000000, 'Emergency medical bills');

    DBMS_OUTPUT.PUT_LINE('--- V_MY_LOANS for Grace (own rows only) ---');
    FOR r IN (SELECT LOAN_ID, MEMBER_NAME, LOAN_AMOUNT, LOAN_STATUS
                FROM V_MY_LOANS ORDER BY LOAN_ID) LOOP
        DBMS_OUTPUT.PUT_LINE('loan ' || r.LOAN_ID || ' | ' || r.MEMBER_NAME ||
                             ' | ' || r.LOAN_AMOUNT || ' | ' || r.LOAN_STATUS);
    END LOOP;

    DBMS_OUTPUT.PUT_LINE('--- SACCO reviews the first PENDING request ---');
    SELECT REQUEST_ID INTO v_req_id FROM LOAN_REQUEST
     WHERE STATUS = 'PENDING' AND ROWNUM = 1 ORDER BY REQUESTED_AT;
    SP_REVIEW_LOAN(v_req_id, 'APPROVED', 'sacco');
    DBMS_OUTPUT.PUT_LINE('Request ' || v_req_id || ' APPROVED - loan recorded.');

    SELECT REQUEST_ID INTO v_req_id FROM LOAN_REQUEST
     WHERE STATUS = 'PENDING' AND ROWNUM = 1 ORDER BY REQUESTED_AT;
    SP_REVIEW_LOAN(v_req_id, 'REJECTED', 'sacco');
    DBMS_OUTPUT.PUT_LINE('Request ' || v_req_id || ' REJECTED.');

    DBMS_OUTPUT.PUT_LINE('--- SACCO (context cleared) sees ALL loans ---');
    PKG_APP_SESSION.SET_MEMBER(NULL);
    FOR r IN (SELECT COUNT(*) AS N FROM V_MY_LOANS) LOOP
        DBMS_OUTPUT.PUT_LINE('SACCO sees ' || r.N || ' loan row(s).');
    END LOOP;
END;
/

SELECT U.USERNAME, U.ROLE, M.FULL_NAME, U.IS_ACTIVE
  FROM APP_USER U LEFT JOIN MEMBER M ON M.MEMBER_ID = U.MEMBER_ID
 ORDER BY U.USER_ID;

SELECT REQUEST_ID, MEMBER_ID, REQUESTED_AMOUNT, PURPOSE, STATUS, REVIEWED_BY
  FROM LOAN_REQUEST ORDER BY REQUEST_ID;

PROMPT SACCOLink multi-user simulation installed.

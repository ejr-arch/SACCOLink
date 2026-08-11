-- ============================================================================
-- SACCOLink (Simplified Prototype)
-- Part IV  : SQL Programming -- demonstration queries
-- Part V   : Cursors -- explicit cursor demo (anonymous block)
--
-- Every category required by the practical paper is demonstrated below with a
-- comment explaining what the query does. Run this file AFTER the schema,
-- packages, triggers and sample data (sql/01, 02, 04, 05, 03, 06) are loaded.
-- ============================================================================

PROMPT ==================== PART IV : SQL PROGRAMMING ====================

-- ----------------------------------------------------------------------------
-- 1. SIMPLE SELECT + WHERE + ORDER BY
-- Members registered in a specific district, newest first.
-- ----------------------------------------------------------------------------
PROMPT -- 1. SELECT with WHERE and ORDER BY
SELECT FULL_NAME, PHONE_NUMBER, DISTRICT
  FROM MEMBER
 WHERE DISTRICT = 'Kampala'
 ORDER BY FULL_NAME ASC;

-- ----------------------------------------------------------------------------
-- 2. AGGREGATE FUNCTIONS + GROUP BY + HAVING
-- Total amount saved per member, but only for members who saved MORE THAN
-- 5,000,000 UGX. The HAVING clause filters groups after aggregation.
-- ----------------------------------------------------------------------------
PROMPT -- 2. GROUP BY + HAVING (groups that saved more than 5M)
SELECT m.FULL_NAME,
       COUNT(s.SAVINGS_ID) AS MONTHS_SAVED,
       NVL(SUM(s.AMOUNT_CONTRIBUTED), 0) AS TOTAL_SAVED
  FROM MEMBER m
  JOIN SAVINGS_RECORD s ON s.MEMBER_ID = m.MEMBER_ID
 GROUP BY m.MEMBER_ID, m.FULL_NAME
HAVING NVL(SUM(s.AMOUNT_CONTRIBUTED), 0) > 5000000
 ORDER BY TOTAL_SAVED DESC;

-- ----------------------------------------------------------------------------
-- 3. JOINS (three tables)
-- Every passport with the member and the current score it was based on.
-- ----------------------------------------------------------------------------
PROMPT -- 3. Three-table INNER JOIN
SELECT p.QR_TOKEN, m.FULL_NAME, m.NIN, s.SCORE_VALUE, s.SCORE_BAND,
       p.PASSPORT_STATUS, p.EXPIRES_AT
  FROM CREDIT_PASSPORT p
  JOIN MEMBER m ON m.MEMBER_ID = p.MEMBER_ID
  JOIN CREDIT_SCORE s ON s.SCORE_ID = p.SCORE_ID
 WHERE p.PASSPORT_STATUS = 'ACTIVE'
 ORDER BY p.EXPIRES_AT ASC;

-- ----------------------------------------------------------------------------
-- 4a. SUBQUERY - scalar subquery
-- Members whose total savings are ABOVE the organisation-wide average.
-- The inner query computes one value used by the outer WHERE.
-- ----------------------------------------------------------------------------
PROMPT -- 4a. Scalar subquery (above-average savers)
SELECT m.FULL_NAME, NVL(SUM(s.AMOUNT_CONTRIBUTED), 0) AS TOTAL_SAVED
  FROM MEMBER m
  JOIN SAVINGS_RECORD s ON s.MEMBER_ID = m.MEMBER_ID
 GROUP BY m.MEMBER_ID, m.FULL_NAME
HAVING NVL(SUM(s.AMOUNT_CONTRIBUTED), 0) >
       (SELECT AVG(AMOUNT_CONTRIBUTED) FROM SAVINGS_RECORD)
 ORDER BY TOTAL_SAVED DESC;

-- ----------------------------------------------------------------------------
-- 4b. SUBQUERY - IN subquery
-- Members who have EVER taken a loan (via a subquery on LOAN_RECORD).
-- ----------------------------------------------------------------------------
PROMPT -- 4b. IN subquery (members who have taken a loan)
SELECT FULL_NAME, PHONE_NUMBER, DISTRICT
  FROM MEMBER
 WHERE MEMBER_ID IN (SELECT DISTINCT MEMBER_ID FROM LOAN_RECORD)
 ORDER BY FULL_NAME;

-- ----------------------------------------------------------------------------
-- 4c. SUBQUERY - correlated subquery
-- Loans that are LARGER than the member's own average loan amount
-- (the inner query is re-evaluated for every outer row).
-- ----------------------------------------------------------------------------
PROMPT -- 4c. Correlated subquery (loans above own average)
SELECT m.FULL_NAME, l.LOAN_AMOUNT, l.LOAN_STATUS
  FROM LOAN_RECORD l
  JOIN MEMBER m ON m.MEMBER_ID = l.MEMBER_ID
 WHERE l.LOAN_AMOUNT >
       (SELECT NVL(AVG(l2.LOAN_AMOUNT), 0)
          FROM LOAN_RECORD l2
         WHERE l2.MEMBER_ID = l.MEMBER_ID)
 ORDER BY m.FULL_NAME;

-- ----------------------------------------------------------------------------
-- 5a. SET OPERATOR - UNION (distinct rows from both queries)
-- Everyone who EITHER has an active passport OR a current score.
-- ----------------------------------------------------------------------------
PROMPT -- 5a. UNION
SELECT MEMBER_ID, 'HAS_ACTIVE_PASSPORT' AS CATEGORY
  FROM CREDIT_PASSPORT WHERE PASSPORT_STATUS = 'ACTIVE'
UNION
SELECT MEMBER_ID, 'HAS_CURRENT_SCORE' AS CATEGORY
  FROM CREDIT_SCORE WHERE IS_CURRENT = 'Y'
ORDER BY MEMBER_ID;

-- ----------------------------------------------------------------------------
-- 5b. SET OPERATOR - INTERSECT (rows in both queries)
-- Members who have BOTH taken a loan AND saved money.
-- ----------------------------------------------------------------------------
PROMPT -- 5b. INTERSECT
SELECT MEMBER_ID FROM LOAN_RECORD
INTERSECT
SELECT MEMBER_ID FROM SAVINGS_RECORD
ORDER BY MEMBER_ID;

-- ----------------------------------------------------------------------------
-- 5c. SET OPERATOR - MINUS (rows in the first but not the second)
-- Members who have a current score but have NEVER generated a passport.
-- ----------------------------------------------------------------------------
PROMPT -- 5c. MINUS
SELECT MEMBER_ID FROM CREDIT_SCORE WHERE IS_CURRENT = 'Y'
MINUS
SELECT MEMBER_ID FROM CREDIT_PASSPORT
ORDER BY MEMBER_ID;

-- ----------------------------------------------------------------------------
-- 6. VIEWS
-- The V_PASSPORT_DETAIL view hides the three-table join from reporting code.
-- ----------------------------------------------------------------------------
PROMPT -- 6. Querying a VIEW
SELECT FULL_NAME, SCORE_BAND, PASSPORT_STATUS
  FROM V_PASSPORT_DETAIL
 WHERE SCORE_BAND = 'EXCELLENT';

-- ----------------------------------------------------------------------------
-- 7. EXPLICIT CURSOR (Part V - cursors, anonymous block)
-- LOOP over every member, printing their name and outstanding ACTIVE loans
-- using an explicit cursor (OPEN / FETCH / CLOSE, %NOTFOUND) and the
-- PKG_SACCOINK.OUTSTANDING_BALANCE function from sql/05_packages.sql.
-- ----------------------------------------------------------------------------
PROMPT -- 7. Explicit cursor demo (anonymous block)
SET SERVEROUTPUT ON
BEGIN
    FOR r IN (SELECT MEMBER_ID, FULL_NAME FROM MEMBER ORDER BY FULL_NAME) LOOP
        DBMS_OUTPUT.PUT_LINE(
            r.FULL_NAME || ' owes ' ||
            TO_CHAR(PKG_SACCOINK.OUTSTANDING_BALANCE(r.MEMBER_ID), 'FM999,999,999')
            || ' UGX on active loans');
    END LOOP;
END;
/

PROMPT ==================== END OF PART IV / V DEMOS ====================

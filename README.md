# SACCOLink — Credit Passport System

Recess 2026 · Makerere University · 25/U/0365
Oracle Database 18c XE · PL/SQL · Java Swing (JDBC)

A credit-passport system for Savings and Credit Co-operative (SACCO)
organisations: a Java Swing desktop GUI backed by an Oracle Database XE
schema, with a multi-user (MEMBER/SACCO) simulation and a loan-request
workflow.

---

## 1. Software Requirements

| Software | Version / Notes |
|----------|-----------------|
| Oracle Database XE | 18c or 21c XE (with a PDB, default service `XEPDB1`) |
| Oracle JDBC driver | `ojdbc11.jar` (bundled in `src/lib/`) |
| Java | JDK 11 or later (tested on JDK 26) |
| SQL*Plus / SQL Developer | For running the install script (optional; SQL*Plus ships with XE) |
| OS | Linux / Windows / macOS (GUI is Java Swing) |

No other libraries are required. The JDBC driver `ojdbc11.jar` is included in
`src/lib/`.

---

## 2. Installation Procedure

### Step 1 — Install Oracle Database XE

Install Oracle Database 18c/21c XE and ensure the default pluggable database
service `XEPDB1` is running on port `1521`.

### Step 2 — Create the schema user (once, as `SYS`)

Run as `SYS`/`SYSDBA`:

```sql
CREATE USER SACCOLINK IDENTIFIED BY sacco123 DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
GRANT CONNECT, RESOURCE TO SACCOLINK;
GRANT CREATE ANY CONTEXT, CREATE ANY VIEW, CREATE SYNONYM TO SACCOLINK;
```

### Step 3 — Install the database (single file)

Run the bundled `saccolink_install.sql` as the `SACCOLINK` schema user. It
contains the schema, constraints, sequences, sample data, views, procedures,
functions, triggers, packages, multi-user layer and Part IV/V demo queries in
the correct order, and runs with zero errors on a fresh XE instance:

```
sqlplus SACCOLINK/sacco123@//localhost:1521/XEPDB1 @saccolink_install.sql
```

### Step 4 — Build the application

From the `src/` folder:

```
./build.sh
```

This compiles all Java sources into `src/out/` with `src/lib/ojdbc11.jar` on
the classpath. Verify `src/lib/ojdbc11.jar` is present before building.

### Step 5 — Run the application

```
./run.sh
```

On startup a **Connect dialog** appears where you enter the Oracle details
(host `localhost`, port `1521`, service `XEPDB1`, user `SACCOLINK`,
password `sacco123`); it tests the connection, then asks for an **application
login** (see below).

To skip the dialog, pass `--url`, `--user` and `--pass`, or export
`DB_HOST`, `DB_PORT`, `DB_SERVICE`, `DB_USER`, `DB_PASS` before running.

---

## 3. Login Credentials

There are two layers of credentials:

1. **Oracle schema login** (used in the Connect dialog / JDBC connection):
   `SACCOLINK` / `sacco123` (created in Step 2).
2. **Application login** (used after connecting, seeded by
   `saccolink_install.sql`):

| Username | Password | Role | Scope |
|----------|----------|------|-------|
| `sacco` | `sacco123` | SACCO | Full access to all pages; reviews/approves loan requests |
| `grace` | `member123` | MEMBER | Own records only |
| `okello` | `member123` | MEMBER | Own records only |
| `hassan` | `member123` | MEMBER | Own records only |
| `amina` | `member123` | MEMBER | Own records only |
| `sarah` | `member123` | MEMBER | Own records only |
| `john` | `member123` | MEMBER | Own records only |
| `betty` | `member123` | MEMBER | Own records only |

**SACCO** sees all member data; **MEMBER** logins are restricted to their own
loans, savings, scores and passports (enforced by `V_MY_*` views and the
`saccolink_ctx` application context).

---

## Project Layout

```
saccolink/
├── saccolink_install.sql       # SINGLE-file database install (deliverable 2)
├── sql/                        # modular development scripts (optional)
├── src/                        # Java Swing GUI (all pages + JDBC)
│   ├── build.sh / run.sh       # compile & run (needs ojdbc11.jar in src/lib)
│   ├── lib/ojdbc11.jar         # Oracle JDBC driver
│   ├── out/                    # compiled classes
│   └── saccolink/              # db connector, DAOs, services, GUI panels
└── docs/
    ├── SACCOLink_Project_Report_Complete.docx    # final report (deliverable 1)
    ├── SACCOLink_Project_Report_Complete.pdf
    ├── SACCOLink_ERD_Database_Design_Simplified.docx
    └── VIDEO_SCRIPT_13min.txt                    # ~13-min demo script
```

## What is included

| Original | Status in prototype |
|----------|---------------------|
| MEMBER | Kept |
| SACCO | Dropped |
| SACCO_MEMBERSHIP | Dropped |
| LOAN_RECORD | Kept |
| SAVINGS_RECORD | Kept |
| MOBILE_MONEY_SUMMARY | Dropped |
| CONSENT_LOG | Dropped → `MEMBER.CONSENT_GIVEN` flag + `CONSENT_AUDIT` audit table |
| CREDIT_SCORE | Kept |
| CREDIT_PASSPORT | Kept |
| VERIFICATION_LOG | Dropped → `CREDIT_PASSPORT.VIEW_COUNT` |

Added for the prototype 

| New table | Purpose |
|-----------|---------|
| CONSENT_AUDIT | DPPA-aware audit trail of consent changes |
| APP_USER | Application logins for the MEMBER/SACCO multi-user simulation |
| LOAN_REQUEST | Loan-request submission and SACCO review/approval workflow |

Also simplified: primary keys use the classic sequence + trigger pattern
(per the project brief); 2-factor scoring (60/40) instead of 5;
a package-based PL/SQL layer instead of loose routines, and a Java Swing GUI
with role-based navigation (SACCO 13 pages, MEMBER 7 pages) instead of APEX.

## App start sequence (database connection flow)

Step by step, what happens between `./run.sh` and the Dashboard:

1. **`src/run.sh`** builds the classpath (`out` + `src/lib/ojdbc11.jar`) and
   launches `saccolink.Main`. It forwards `DB_HOST/DB_PORT/DB_SERVICE/DB_USER/
   DB_PASS` as `--url/--user/--pass`, so nothing extra is typed when the
   environment is set. `--url` and `--user` are only passed together; a lone
   `DB_USER` without host/port/service prints a warning.
2. **`Main.main`** (Swing EDT) applies the Metal look-and-feel + 13 pt default
   font, creates the `MainFrame`, then either
   - applies the command-line configuration via `DBConnection.configureRaw`
     (skips the dialog), or
   - shows the **Connect dialog** (`ConnectionDialog`) with host, port (1521),
     service name (XEPDB1), schema user (SACCOLINK) and password. "Test
     Connection" builds the thin JDBC URL
     `jdbc:oracle:thin:@//host:port/service` and calls
     `DBConnection.testConnection()`; failures show a readable message and the
     dialog stays open.
3. **`DBConnection`** owns the JDBC settings (URL / user / password). Every
   `getConnection()` registers `oracle.jdbc.OracleDriver` and opens a fresh
   physical connection; a **15-second login timeout** (`DriverManager.
   setLoginTimeout`) makes an unreachable host fail fast instead of freezing
   the UI. Every DAO call uses try-with-resources, so connections are always
   closed.
4. **Login dialog** (`LoginDialog`) calls `SP_LOGIN` through
   `UserService.login` on a background thread — the UI never blocks.
   `SP_LOGIN` checks the username against the SHA-256 hash of
   `username#password` in `APP_USER`, returns user id / role / member id /
   display name, and sets the application context.
5. **Session + row security**: `Session.setUser` keeps the logged-in app user.
   Every scoped read opens its connection via `Session.openScopedConnection()`,
   which first runs `PKG_APP_SESSION.SET_MEMBER(:memberId)` to set
   `saccolink_ctx.MEMBER_ID` — `NULL` for SACCO (sees all rows), the member id
   for a MEMBER. The `V_MY_LOANS` / `V_MY_SAVINGS` / `V_MY_SCORES` /
   `V_MY_PASSPORTS` / `V_MY_PROFILE` views filter on that context, so a member
   can never read another member's rows even though everyone shares the
   `SACCOLINK` Oracle account.
6. **`MainFrame.applySession`** rebuilds the sidebar for the role (SACCO 13
   pages, MEMBER 7), reloads every panel (`refreshAll`) and opens the
   Dashboard. Data is loaded only after login — connecting without logging in
   only runs the connection test.
7. **File > Reconnect** re-shows the Connect dialog and refreshes every panel
   when already logged in (e.g. after an Oracle container restart).

### Connection failure handling
- Wrong host / port / service or bad schema credentials → Connect dialog shows
  the SQL error and stays open for a retry.
- Oracle JDBC driver missing from `src/lib/` → "Oracle JDBC driver not found.
  Place ojdbc11.jar in src/lib/".
- Database down while the app is already running → panels catch `SQLException`
  and show a "Database error" dialog; restart the DB, then use
  File > Reconnect.
- `--url` without `--user` (or vice versa) → warning dialog, then falls back
  to the Connect dialog.

### Troubleshooting — `saccolink_ctx` ownership (ORA-01031)
The application context is a database-global namespace created by the install
script (`CREATE CONTEXT saccolink_ctx USING PKG_APP_SESSION`) and belongs to
the schema that ran the install. If logins suddenly fail with
`ORA-01031: insufficient privileges` at `PKG_APP_SESSION.SET_MEMBER`, the
namespace is owned by the wrong schema (typically the install was re-run from
a different user). Fix as `SYS`:
```sql
DROP CONTEXT saccolink_ctx;
```
then, as `SACCOLINK`:
```sql
CREATE CONTEXT saccolink_ctx USING PKG_APP_SESSION;
```

## Multi-user login (APP_USER)

Two account types are simulated via the `APP_USER` table (application login,
separate from the Oracle schema user):

- **MEMBER** – sees ONLY their own loans / savings / scores / passports
  (enforced by the `V_MY_*` views + `saccolink_ctx` application context).
  Can request a loan and generate a passport token for a creditor.
- **SACCO** – full access to every page; reviews member loan requests,
  checks creditworthiness (score + loan/savings totals) and approves or
  rejects them (approval records the loan on `LOAN_RECORD` as ACTIVE).

Login credentials are stored SHA-256 hashed (`FN_HASH_PASSWORD`). Users are
seeded in `saccolink_install.sql` (originally `sql/06_multiuser.sql`):
`username` = `sacco` (password `sacco123`) or a member first name
(e.g. `grace`, password `member123`).

## Coverage

- **Part III** – tables, PK/FK/unique/check constraints, defaults, indexes,
  sequences, **synonyms** (`CLIENTS`, `LOANS`, `SAVINGS`, `MEMBER_DETAIL`) and
  realistic sample data.
- **Part IV** – every query type demonstrated in `saccolink_install.sql`
  (Part IV/V section): simple SELECT, WHERE, ORDER BY, GROUP BY, **HAVING**,
  aggregate functions, joins, scalar/IN/correlated **subqueries**,
  **UNION / INTERSECT / MINUS**, views, each with an explanation.
- **Part V** – procedures, functions, packages, anonymous blocks, exception
  handling, triggers and an **explicit cursor**
  (`PKG_SACCOINK.OUTSTANDING_BALANCE` uses `CURSOR / OPEN / FETCH / %ROWTYPE /
  CLOSE`).
- **Part VI** – user login, **Dashboard** (SACCO org totals / member summary),
  entry/edit/delete forms, search, reports (Savings Report with total),
  navigation menu, input validation and error messages.
- **Part VII** – MEMBER / SACCO application users, SHA-256 password hashing,
  per-user row security via the `saccolink_ctx` context, and role-based GUI
  access (backup strategy is documented in the report).
- **Deliverable 2** – `saccolink_install.sql` (a single SQL file that runs with
  zero errors on a fresh Oracle Database XE installation).

## Scoring formula

- Loan Repayment (60%): repaid/total × 100, −25 per default
- Savings Consistency (40%): months contributed / months since first × 100
- Composite = (Repayment × 0.60 + Savings × 0.40) × 8.5 → 0–850
- Bands: EXCELLENT ≥ 700 · GOOD 550–699 · FAIR 400–549 · THIN < 400

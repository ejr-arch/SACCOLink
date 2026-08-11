# JDBC Driver

Place the Oracle JDBC driver JAR in this directory before building:

- `ojdbc11.jar` (Oracle Database 19c, JDK 11+)

Download from Oracle (free login required):
https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html

The app also auto-loads any `*.jar` in this folder via a
`DriverManager` service-load fallback, so the H2 in-memory driver
(`h2-2.*.jar`) can be dropped here too for offline demos.

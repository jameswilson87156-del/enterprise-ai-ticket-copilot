# Versioned staging database operations

The application has no automatic migration runner. It keeps SQL init disabled;
restarting an API or Compose stack never substitutes for a database upgrade.
No new production dependency was introduced by this remediation.

## Initial version

`database/V1__schema_only.sql` is a new empty-database baseline. It contains no
CREATE DATABASE/USE directive, no Demo knowledge or tickets, and no DELETE/DROP.
The operator explicitly selects the independent target database. Strict CREATE
TABLE statements reject replay instead of silently accepting an existing schema.
The final statement records version 1 in `ticket_schema_history` only after all
earlier SQL statements succeed. `database/manifest.json` holds the exact SHA-256.

Before an explicitly authorized deployment:

1. Verify the release manifest checksum matches the SQL file; do not edit a
   released/applied file. Obtain an empty database and a migration-only account
   scoped to that database. Existing databases require an independent adoption
   review; do not run this baseline on them.
2. Keep the migration client's credentials in a protected MySQL option file on
   the server. Use verified TLS and the correct RDS CA. Never put a password in
   command arguments or chat. The selected database must be the ticket database.
3. Execute the reviewed SQL with the standard MySQL client in batch mode,
   without `--force`. Capture exit status. Only exit 0 plus the expected schema
   version and empty business-table checks permit API startup.
4. Run the API with its own DML account using `TICKET_DB_URL/USERNAME/PASSWORD`.
   Provision synthetic knowledge and users separately after review. Do not use
   legacy `schema.sql` or `demo-data.sql` as production initialization.

## Subsequent versions and failure handling

Add one reviewed V2/V3 SQL file at a time and extend the immutable checksum
manifest. Before execution verify that `ticket_schema_history` contains exactly
the expected preceding versions and that the previous release checksums match.
Each new script records its version only as its final statement. Only one
operator/release job may hold the deployment window; do not run concurrent
migrations. Capture backup ID, pre/post version, checksum, operator, timestamps
and image digest in the project release record.

MySQL DDL can commit partially. On any failure stop API startup and retain the
database and logs; the missing history row is not permission to rerun or to
delete tables. Investigate and recover to a separate database from a verified
backup when necessary. Rehearse every upgrade against a restored temporary
database, checking copilot_run, retrieval_hit, review_record and citation links.
Only roll back the application if its previous version supports the new schema.

This is an explicit manual versioned procedure, not an automated migration or
cloud backup acceptance claim. Cloud execution, existing database adoption and
backup/restore remain STAGING_PENDING.

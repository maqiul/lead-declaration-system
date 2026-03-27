#!/bin/bash
IGNORE=""
for t in $(mysql -u root -p6678722mm -N -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='lead_declaration' AND (TABLE_NAME LIKE 'ACT_%' OR TABLE_NAME LIKE 'FLW_%');" 2>/dev/null); do
  IGNORE="$IGNORE --ignore-table=lead_declaration.$t"
done
mysqldump -u root -p6678722mm --single-transaction --set-gtid-purged=OFF lead_declaration $IGNORE 2>/dev/null

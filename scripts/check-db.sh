#!/usr/bin/env bash
#
# Reports what actually exists in the target database. Read-only — it never writes.
#
# The connection string is read from the environment or from backend/.env.local (which
# is gitignored), so the password is never passed on the command line where it would
# land in shell history or `ps` output.
#
# Usage:
#   echo 'DATABASE_URL=postgresql://user:pass@host:5432/postgres' > .env.local
#   ./scripts/check-db.sh
#
# Or:  DATABASE_URL='postgresql://...' ./scripts/check-db.sh

set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

# ---------------------------------------------------------------- load credentials
if [[ -z "${DATABASE_URL:-}" && -f .env.local ]]; then
  # shellcheck disable=SC1091
  set -a; source .env.local; set +a
fi

if [[ -z "${DATABASE_URL:-}" ]]; then
  cat >&2 <<'EOF'
DATABASE_URL is not set.

Get it from Supabase: Project Settings -> Database -> Connection string -> Session mode
(port 5432, the "pooler" host — not the direct db.<ref>.supabase.co host, which is
IPv6-only), then either:

  echo 'DATABASE_URL=postgresql://postgres.<ref>:<password>@aws-0-<region>.pooler.supabase.com:5432/postgres' > .env.local

or export it for one run. .env.local is gitignored.
EOF
  exit 1
fi

# Never print the URL itself; show only the host so mistakes are still diagnosable.
SAFE_HOST=$(printf '%s' "$DATABASE_URL" | sed -E 's|^.*@([^/?]+).*$|\1|')

# Fail fast instead of hanging on an unreachable host.
export PGCONNECT_TIMEOUT=10

q() { psql "$DATABASE_URL" -X -A -t -q -c "$1" 2>&1; }

echo "Target: $SAFE_HOST"
echo

# ------------------------------------------------------------------ connectivity
if ! server_version=$(q "SELECT version();"); then
  echo "CANNOT CONNECT"
  echo "$server_version"
  exit 1
fi
if [[ "$server_version" != PostgreSQL* ]]; then
  echo "CANNOT CONNECT"
  echo "$server_version"
  exit 1
fi
echo "Connected: ${server_version%% on *}"
echo

# ------------------------------------------------------------------- app tables
echo "── Application tables ──────────────────────────────────────────"
expected=(students parents parent_weekly_reports timetables timetable_slots)
missing=0
for t in "${expected[@]}"; do
  exists=$(q "SELECT to_regclass('public.$t') IS NOT NULL;")
  if [[ "$exists" == "t" ]]; then
    rows=$(q "SELECT count(*) FROM public.$t;")
    printf '  %-24s present   %s rows\n' "$t" "$rows"
  else
    printf '  %-24s MISSING\n' "$t"
    missing=$((missing + 1))
  fi
done
echo

# ------------------------------------------------------------- migration history
echo "── Flyway history ──────────────────────────────────────────────"
if [[ "$(q "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL;")" == "t" ]]; then
  q "SELECT '  V' || version || '  ' || rpad(description, 34) ||
            CASE WHEN success THEN 'ok' ELSE 'FAILED' END
     FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank;"
else
  echo "  No flyway_schema_history table — Flyway has never run here."
  if (( missing < ${#expected[@]} )); then
    echo "  Tables exist without it, so Hibernate's ddl-auto=update created them."
    echo "  baseline-on-migrate will then SKIP V1 and apply only V2/V3."
  fi
fi
echo

# ------------------------------------------------- accounts affected by V3
echo "── Passwords (V3 clears every non-BCrypt value) ────────────────"
for t in students parents; do
  if [[ "$(q "SELECT to_regclass('public.$t') IS NOT NULL;")" == "t" ]]; then
    if [[ "$(q "SELECT count(*) FROM information_schema.columns
                WHERE table_schema='public' AND table_name='$t' AND column_name='password';")" == "1" ]]; then
      q "SELECT '  $t: ' || count(*) || ' total, ' ||
                count(*) FILTER (WHERE password LIKE '\$2%')  || ' hashed, ' ||
                count(*) FILTER (WHERE password IS NOT NULL
                                   AND password NOT LIKE '\$2%') || ' PLAINTEXT (will be cleared), ' ||
                count(*) FILTER (WHERE password IS NULL)      || ' none'
         FROM public.$t;"
    else
      echo "  $t: no password column yet (pre-V2 schema)"
    fi
  fi
done
echo

# --------------------------------------------------------------------- verdict
echo "── Verdict ─────────────────────────────────────────────────────"
if (( missing == ${#expected[@]} )); then
  echo "  Empty database. Flyway will create everything on first boot. Safe to deploy."
elif (( missing > 0 )); then
  echo "  Partial schema — $missing of ${#expected[@]} tables missing."
  echo "  ddl-auto=validate will refuse to start. Resolve before deploying."
else
  echo "  Schema is present. Check the plaintext count above: those accounts lose"
  echo "  their password on deploy and cannot recover it — there is no reset flow."
fi

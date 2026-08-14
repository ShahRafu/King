#!/usr/bin/env bash
set -euo pipefail

# scripts/self_heal.sh
# Conservative self-heal: run Gradle build, parse compiler errors to find affected files,
# run safe sanitizers on each affected file, re-run the build, and (with a valid
# approval token) commit the fixes to a new branch.

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || echo "$(pwd)")
SELFHEAL_DIR="$REPO_ROOT/.selfheal"
BACKUP_DIR="$SELFHEAL_DIR/backups"
REPORTS_DIR="$SELFHEAL_DIR/reports"
APPROVAL_DIR="$SELFHEAL_DIR/approvals"
LAST_LOG="$SELFHEAL_DIR/last-build.log"

mkdir -p "$SELFHEAL_DIR" "$BACKUP_DIR" "$REPORTS_DIR" "$APPROVAL_DIR"

TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
REPORT_JSON="$REPORTS_DIR/report-$TIMESTAMP.json"

echo "[self_heal] Starting self-heal run at $TIMESTAMP"

# Run Gradle build and capture output
echo "[self_heal] Running ./gradlew assembleDebug"
if ./gradlew assembleDebug > "$LAST_LOG" 2>&1; then
  echo "[self_heal] Build succeeded — nothing to do"
  exit 0
fi

echo "[self_heal] Build failed — parsing errors"

# Parse Kotlin/Java error file paths. Support file:/// and plain paths ending with .kt or .java
# We'll extract occurrences like /path/to/File.kt:line:col or file:///.../File.kt:line:col
FILES_FOUND=$(grep -oE "(file://[^:]+\.(kt|java)):[0-9]+:[0-9]+|([[:alnum:]/._-]+\.(kt|java)):[0-9]+:[0-9]+" "$LAST_LOG" || true)

if [ -z "$FILES_FOUND" ]; then
  echo "[self_heal] No affected source files found by parser. Writing full log to $REPORT_JSON"
  cat > "$REPORT_JSON" <<EOF
{ "timestamp": "$TIMESTAMP", "status": "failed", "reason": "no_affected_files_detected", "log": "$(sed 's/"/\\\"/g' "$LAST_LOG")" }
EOF
  echo "[self_heal] Report written to $REPORT_JSON"
  exit 1
fi

# Normalize file paths and deduplicate
AFFECTED_FILES=()
while IFS= read -r line; do
  # extract the path before the first ':'
  path=$(echo "$line" | sed -E 's/^file:\/\///; s/:.*$//')
  # If path is relative, try to convert to repo-root relative path
  if [ -f "$path" ]; then
    AFFECTED_FILES+=("$path")
  else
    # try repo-root relative
    candidate="$REPO_ROOT/$path"
    if [ -f "$candidate" ]; then
      AFFECTED_FILES+=("$candidate")
    else
      # ignore non-existent files
      echo "[self_heal] WARNING: referenced file not found: $path"
    fi
  fi
done <<< "$(echo "$FILES_FOUND" | sort -u)"

if [ ${#AFFECTED_FILES[@]} -eq 0 ]; then
  echo "[self_heal] No existing affected files were found on disk. Aborting."
  exit 1
fi

echo "[self_heal] Affected files:" >&2
for f in "${AFFECTED_FILES[@]}"; do echo " - $f"; done

# Back up and sanitize each affected file
CHANGED_FILES=()
for f in "${AFFECTED_FILES[@]}"; do
  rel=${f#$REPO_ROOT/}
  backup_dir="$BACKUP_DIR/$TIMESTAMP/$(dirname "$rel")"
  mkdir -p "$backup_dir"
  cp "$f" "$backup_dir/$(basename "$f")"
  echo "[self_heal] Backed up $rel -> $backup_dir/"

  # Apply deterministic sanitizers (safe edits):
  # 1) Remove accidental metadata header line matching ^\s*name=.*url=.*$
  # 2) Remove UTF-8 BOM if present
  # 3) Normalize CRLF to LF
  tmp="$f.selfheal.tmp"
  awk 'NR==1 && /^\s*name=.*url=.*$/ {next} {print}' "$f" > "$tmp" || true
  # Remove BOM (0xEF 0xBB 0xBF)
  sed -i '1s/^\xEF\xBB\xBF//' "$tmp" || true
  # Normalize EOL
  sed -i 's/\r$//' "$tmp" || true

  # If tmp differs from original, overwrite original and record
  if ! cmp -s "$f" "$tmp"; then
    mv "$tmp" "$f"
    CHANGED_FILES+=("$rel")
    echo "[self_heal] Sanitized $rel"
  else
    rm -f "$tmp"
    echo "[self_heal] No sanitization needed for $rel"
  fi
done

# Re-run the build
echo "[self_heal] Re-running build after sanitization"
if ./gradlew assembleDebug > "$LAST_LOG" 2>&1; then
  echo "[self_heal] Build succeeded after sanitization"
  result_status="fixed"
else
  echo "[self_heal] Build still failing after sanitization. See $LAST_LOG"
  result_status="still_failing"
fi

# Prepare report
python - <<PY
import json, time
report = {
  'timestamp': '$TIMESTAMP',
  'status': '$result_status',
  'changed_files': ${json.dumps(CHANGED_FILES)},
}
open('$REPORT_JSON','w').write(json.dumps(report, indent=2))
print('Wrote report to $REPORT_JSON')
PY

# If fixed and approval token exists and valid, commit changes to new branch
if [ "$result_status" = "fixed" ] && [ ${#CHANGED_FILES[@]} -gt 0 ]; then
  # check for approval token
  TOKEN_FILE="$(ls $APPROVAL_DIR/*.json 2>/dev/null | tail -n1 || true)"
  if [ -n "$TOKEN_FILE" ]; then
    echo "[self_heal] Found approval token: $TOKEN_FILE — validating expiry"
    # Validate expiry via python
    valid=$(python - <<PY
import json, time
try:
  t=json.load(open('$TOKEN_FILE'))
  exp=int(t.get('expires',0))
  print(1 if exp>int(time.time()) else 0)
except Exception:
  print(0)
PY
)
    if [ "$valid" = "1" ]; then
      branch="selfheal/$TIMESTAMP"
      echo "[self_heal] Approval token valid — committing changes to branch $branch"
      git fetch origin || true
      git checkout -b "$branch"
      for cf in "${CHANGED_FILES[@]}"; do git add -- "$cf" || true; done
      git commit -m "selfheal: automated sanitization fixes ($TIMESTAMP)" || true
      # Try to push branch; ignore failures (may not have permissions)
      if git push -u origin "$branch"; then
        echo "[self_heal] Pushed branch $branch to origin"
      else
        echo "[self_heal] Warning: push failed (no permissions?) — branch created locally: $branch"
      fi
    else
      echo "[self_heal] Approval token expired or invalid — skipping commit. You can apply manually."
    fi
  else
    echo "[self_heal] No approval token found — skipping commit. Place a valid token in $APPROVAL_DIR to enable automatic commit."
  fi
fi

echo "[self_heal] Done. Report: $REPORT_JSON"
exit 0

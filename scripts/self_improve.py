#!/usr/bin/env python3
"""
scripts/self_improve.py

Simple local agent stub (Conservative MVP).
- Accepts a short natural language instruction on the CLI.
- Creates a candidate proposal by running a dry-run self_heal (no token commit) and collecting results.
- Writes a human-readable proposal JSON into .selfheal/proposals/

This is a starter script; advanced generation (AI/model) is intentionally out of scope for the conservative MVP.

Usage:
  ./scripts/self_improve.py "Add a voice command to set budget"

The script requires:
  - a Unix-like shell environment
  - git available in PATH
  - ./scripts/self_heal.sh present and executable

"""

import json
import os
import shlex
import subprocess
import sys
import time
from pathlib import Path

REPO_ROOT = Path.cwd()
SELFHEAL = REPO_ROOT / ".selfheal"
PROPOSALS = SELFHEAL / "proposals"

os.makedirs(PROPOSALS, exist_ok=True)

if len(sys.argv) < 2:
    print("Usage: scripts/self_improve.py \"<instruction>\"")
    sys.exit(2)

instruction = sys.argv[1]
print(f"[self_improve] Instruction: {instruction}")

timestamp = time.strftime("%Y%m%dT%H%M%SZ", time.gmtime())
proposal_file = PROPOSALS / f"proposal-{timestamp}.json"

# 1) Run self_heal in DRY-RUN mode. The conservative self_heal.sh only auto-commits with token;
#    so running it as-is will sanitize and write a report but will not commit if no token is present.
print("[self_improve] Running conservative self_heal dry-run...")
try:
    subprocess.run(["bash", "./scripts/self_heal.sh"], check=True)
except subprocess.CalledProcessError:
    # self_heal returns non-zero if it couldn't fix or other conditions; we still continue to collect report
    print("[self_improve] self_heal completed (non-zero exit); continuing to gather report")

# 2) Collect the latest report (if present)
reports_dir = SELFHEAL / "reports"
latest_report = None
if reports_dir.exists():
    reports = sorted(reports_dir.glob('report-*.json'))
    if reports:
        latest_report = reports[-1]

report_data = None
if latest_report:
    try:
        report_data = json.loads(latest_report.read_text())
    except Exception:
        report_data = {"error":"failed_to_read_report"}

# 3) Collect git diff (uncommitted changes)
try:
    diff = subprocess.check_output(["git", "--no-pager", "diff", "--"]).decode('utf-8')
except Exception:
    diff = ""

proposal = {
    "timestamp": timestamp,
    "instruction": instruction,
    "report": report_data,
    "uncommitted_diff": diff,
    "note": "This is a conservative proposal. No commits were created. To apply changes automatically, generate a valid approval token and re-run the self_heal script."
}

proposal_file.write_text(json.dumps(proposal, indent=2))
print(f"[self_improve] Proposal written to {proposal_file}")
print("[self_improve] Summary:")
print(json.dumps({"timestamp": timestamp, "report_summary": report_data}, indent=2))

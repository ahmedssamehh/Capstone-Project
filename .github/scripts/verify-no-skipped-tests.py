#!/usr/bin/env python3
"""
Parse Maven Surefire XML reports and fail CI on skips, failures, or errors.

Usage:
  python verify-no-skipped-tests.py
  python verify-no-skipped-tests.py --min-tests 30 --label integration
"""
from __future__ import annotations

import argparse
import glob
import os
import sys
import xml.etree.ElementTree as ET

REPORTS_DIR = "target/surefire-reports"
PATTERN = f"{REPORTS_DIR}/TEST-*.xml"


def parse_reports() -> tuple[int, int, int, int, list[str]]:
    files = glob.glob(PATTERN)
    details: list[str] = []
    total_tests = total_failures = total_errors = total_skipped = 0

    for path in sorted(files):
        root = ET.parse(path).getroot()
        classname = root.attrib.get("name", path)
        tests = int(root.attrib.get("tests", "0"))
        failures = int(root.attrib.get("failures", "0"))
        errors = int(root.attrib.get("errors", "0"))
        skipped = int(root.attrib.get("skipped", "0"))

        total_tests += tests
        total_failures += failures
        total_errors += errors
        total_skipped += skipped

        if tests > 0:
            details.append(
                f"| `{classname}` | {tests} | {failures} | {errors} | {skipped} |"
            )

    return total_tests, total_failures, total_errors, total_skipped, details


def write_step_summary(label: str, total: int, failures: int, errors: int, skipped: int, rows: list[str]) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return

    status = "PASS" if not (failures or errors or skipped) and total > 0 else "FAIL"
    with open(summary_path, "a", encoding="utf-8") as fh:
        fh.write(f"## Surefire — {label}\n\n")
        fh.write(f"**Status:** {status}\n\n")
        fh.write(f"| Metric | Count |\n|--------|------:|\n")
        fh.write(f"| Tests | {total} |\n| Failures | {failures} |\n| Errors | {errors} |\n| Skipped | {skipped} |\n\n")
        if rows:
            fh.write("| Class | Tests | Failures | Errors | Skipped |\n")
            fh.write("|-------|------:|---------:|-------:|--------:|\n")
            fh.write("\n".join(rows))
            fh.write("\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify Surefire results for CI.")
    parser.add_argument("--min-tests", type=int, default=1, help="Minimum executed tests required.")
    parser.add_argument("--label", type=str, default="tests", help="Job label for step summary.")
    args = parser.parse_args()

    files = glob.glob(PATTERN)
    if not files:
        print(f"ERROR: No Surefire reports at {PATTERN}")
        return 1

    total, failures, errors, skipped, rows = parse_reports()

    print(
        f"[{args.label}] tests={total} failures={failures} errors={errors} skipped={skipped} "
        f"(reports={len(files)})"
    )

    write_step_summary(args.label, total, failures, errors, skipped, rows)

    if failures or errors:
        print("ERROR: Test failures or errors detected.")
        return 1

    if skipped > 0:
        print("ERROR: Skipped tests not allowed (Docker/Testcontainers likely unavailable).")
        return 1

    if total < args.min_tests:
        print(f"ERROR: Expected at least {args.min_tests} tests, but only {total} ran.")
        return 1

    print("OK: All tests executed with zero skips, failures, and errors.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

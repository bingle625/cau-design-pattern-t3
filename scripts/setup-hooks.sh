#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source .githooks/lib/output.sh
heading 'Git hooks setup'
current=$(git config --get core.hooksPath || true)
if [ -n "$current" ] && [ "$current" != .githooks ]; then
    fail "An existing hooksPath is configured: $current"
    printf 'Review that setup before replacing it with .githooks.\n' >&2
    exit 1
fi
chmod +x .githooks/pre-commit .githooks/commit-msg scripts/setup-hooks.sh scripts/run-gradle.sh
git config --local core.hooksPath .githooks
success 'Hooks enabled for this checkout'
step 'Pre-commit: staged whitespace, conflict markers, JUnit'
step 'Commit message: feat | fix | refactor | test | docs | chore'

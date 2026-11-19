#!/usr/bin/env bash
# Color only in interactive terminals; keep IDE/CI logs readable.
bold='' green='' red='' dim='' reset=''
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ] && [ "${TERM:-}" != dumb ]; then
    bold=$'\033[1m'; green=$'\033[32m'; red=$'\033[31m'
    dim=$'\033[2m'; reset=$'\033[0m'
fi
heading() { printf '\n%sHolub Life | %s%s\n\n' "$bold" "$1" "$reset"; }
step() { printf '  %s...%s %s\n' "$dim" "$reset" "$1"; }
success() { printf '  %s[OK]%s %s\n' "$green" "$reset" "$1"; }
fail() { printf '  %s[FAIL]%s %s\n' "$red" "$reset" "$1" >&2; }

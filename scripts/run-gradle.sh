#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

java_is_supported() {
    local settings major
    settings=$("$1" -XshowSettings:properties -version 2>&1) || return 1
    major=$(printf '%s\n' "$settings" | awk '$1 == "java.specification.version" { sub(/^1\./, "", $3); print $3; exit }')
    [[ "$major" =~ ^[0-9]+$ ]] && (( major >= 17 ))
}

# Respect an explicit JAVA_HOME, but explain an incompatible selection.
if [ -n "${JAVA_HOME:-}" ]; then
    if ! java_is_supported "$JAVA_HOME/bin/java"; then
        printf 'JAVA_HOME must point to a working JDK 17 or newer. Update JAVA_HOME and try again.\n' >&2
        exit 1
    fi
elif ! java_is_supported java; then
    # Also search when PATH contains an older Java, not just a missing one.
    if [ "$(uname -s)" = Darwin ]; then
        candidate=$(/usr/libexec/java_home -v '17+' 2>/dev/null || true)
        if [ -n "$candidate" ] && java_is_supported "$candidate/bin/java"; then
            export JAVA_HOME="$candidate"
        else
            for candidate in /opt/homebrew/opt/openjdk*/libexec/openjdk.jdk/Contents/Home /usr/local/opt/openjdk*/libexec/openjdk.jdk/Contents/Home; do
                if java_is_supported "$candidate/bin/java"; then
                    export JAVA_HOME="$candidate"
                    break
                fi
            done
        fi
    fi
    if [ -z "${JAVA_HOME:-}" ]; then
        printf 'No working JDK 17 or newer found. Install one or set JAVA_HOME, then try again.\n' >&2
        exit 1
    fi
fi
exec bash ./gradlew "$@"

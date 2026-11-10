#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Respect an explicit JAVA_HOME. Also support Homebrew JDKs that have not
# been registered with macOS (common in terminals and Git GUI clients).
if [ -z "${JAVA_HOME:-}" ] && ! java -version >/dev/null 2>&1; then
    if [ "$(uname -s)" = Darwin ]; then
        candidate=$(/usr/libexec/java_home -v '17+' 2>/dev/null || true)
        if [ -n "$candidate" ]; then
            export JAVA_HOME="$candidate"
        else
            for candidate in /opt/homebrew/opt/openjdk*/libexec/openjdk.jdk/Contents/Home /usr/local/opt/openjdk*/libexec/openjdk.jdk/Contents/Home; do
                if [ -x "$candidate/bin/java" ]; then
                    export JAVA_HOME="$candidate"
                    break
                fi
            done
        fi
    fi
fi
exec bash ./gradlew "$@"

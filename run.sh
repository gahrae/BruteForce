#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# Locate javac and java. Prefer JAVA_HOME, fall back to PATH.
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
  JAVAC="$JAVA_HOME/bin/javac"
  JAVA="$JAVA_HOME/bin/java"
elif command -v javac >/dev/null 2>&1; then
  JAVAC="javac"
  JAVA="java"
else
  echo "error: javac not found" >&2
  echo "install a JDK (not just a JRE) and either put it on PATH or set JAVA_HOME" >&2
  exit 1
fi

rm -rf out
mkdir -p out
"$JAVAC" -d out *.java

echo "=== Tests ==="
"$JAVA" -cp out Tests
echo
echo "=== Demo ==="
"$JAVA" -cp out Demo

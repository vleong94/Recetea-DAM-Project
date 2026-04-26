#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Recetea — native packaging script (Linux / macOS)
#
# Produces a platform-specific installer via jlink + jpackage:
#   Linux  → .deb (requires dpkg-deb) or .rpm (requires rpm-build)
#   macOS  → .dmg or .pkg (requires Xcode CLI tools)
#
# Prerequisites:
#   • JDK 24 (Amazon Corretto recommended) — export JAVA_HOME before running.
#   • PostgreSQL at localhost:5432 for integration tests; pass --skip-tests to bypass.
#
# Usage:
#   ./package.sh [--skip-tests]
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SKIP_TESTS=false
for arg in "$@"; do
  [[ "$arg" == "--skip-tests" ]] && SKIP_TESTS=true
done

# ── Detect JAVA_HOME ──────────────────────────────────────────────────────────
if [[ -z "${JAVA_HOME:-}" ]]; then
  CORRETTO="/c/Users/$(whoami)/.jdks/corretto-24.0.2"
  if [[ -d "$CORRETTO" ]]; then
    export JAVA_HOME="$CORRETTO"
  else
    echo "Error: JAVA_HOME is not set. Export it before running this script." >&2
    exit 1
  fi
fi
echo "Using JAVA_HOME: $JAVA_HOME"

# ── Detect package type ───────────────────────────────────────────────────────
case "$(uname -s)" in
  Linux*)  PKG_TYPE="deb" ;;
  Darwin*) PKG_TYPE="dmg" ;;
  *)       PKG_TYPE="app-image" ;;
esac
echo "Target package type: $PKG_TYPE"

MVN_FLAGS=""
[[ "$SKIP_TESTS" == "true" ]] && MVN_FLAGS="-DskipTests"

# ── Step 1: Build ─────────────────────────────────────────────────────────────
echo ""
echo "[1/3] Building project..."
JAVA_HOME="$JAVA_HOME" ./mvnw clean package $MVN_FLAGS

# ── Step 2: jlink — stripped JRE image ────────────────────────────────────────
echo ""
echo "[2/3] Creating jlink runtime image..."
JAVA_HOME="$JAVA_HOME" ./mvnw javafx:jlink

IMAGE_DIR="target/recetea-runtime"
if [[ -d "$IMAGE_DIR" ]]; then
  SIZE=$(du -sh "$IMAGE_DIR" | cut -f1)
  echo "    Runtime image size: $SIZE"
fi

# ── Step 3: jpackage — platform installer ────────────────────────────────────
echo ""
echo "[3/3] Packaging platform installer (type=$PKG_TYPE)..."
# Override jpackageType for non-Windows targets
JAVA_HOME="$JAVA_HOME" ./mvnw javafx:jpackage -Djavafx.jpackageType="$PKG_TYPE"

echo ""
echo "Done. Installer written to target/installer/"
ls -lh target/installer/ 2>/dev/null || true

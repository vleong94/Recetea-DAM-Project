#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Recetea — portable app-image packaging (Linux / macOS / Windows-bash)
#
# Default output: target/installer/Recetea/ — a self-contained portable folder
# (Recetea launcher + bundled JRE + config). Copy anywhere, run in place.
# No installation step, no platform-specific tooling.
#
# Override via --type <kind> for native installers:
#   --type deb    Linux .deb  (needs dpkg-deb)
#   --type rpm    Linux .rpm  (needs rpm-build)
#   --type dmg    macOS .dmg  (needs Xcode CLI tools)
#   --type msi    Windows .msi (needs WiX Toolset 3.x on PATH)
#
# Prerequisites:
#   • JDK 24 (Amazon Corretto recommended) — export JAVA_HOME before running.
#   • PostgreSQL at localhost:5432 for integration tests; pass --skip-tests to bypass.
#
# Usage:
#   ./package.sh                    # portable app-image folder (default)
#   ./package.sh --skip-tests       # portable folder, skip tests
#   ./package.sh --type deb         # native .deb installer
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SKIP_TESTS=false
PKG_TYPE="app-image"
while (( $# )); do
  case "$1" in
    --skip-tests) SKIP_TESTS=true ;;
    --type)       PKG_TYPE="${2:-}"; shift ;;
    --type=*)     PKG_TYPE="${1#--type=}" ;;
    *)            echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
  shift
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
echo "Target package type: $PKG_TYPE"

MVN_FLAGS=""
[[ "$SKIP_TESTS" == "true" ]] && MVN_FLAGS="-DskipTests"

M2="${HOME}/.m2/repository"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) M2="${USERPROFILE//\\//}/.m2/repository" ;;
esac

# ── Step 0: Moditect bootstrap (one-shot per fresh m2 cache) ──────────────────
# jlink rejects automatic modules. We patch four jars (bcrypt, bytes,
# postgresql, openpdf) via Moditect so each carries an explicit module-info.
# Detect a cold cache by probing the bytes jar.
BYTES_JAR="${M2}/at/favre/lib/bytes/1.5.0/bytes-1.5.0.jar"
NEEDS_BOOTSTRAP=true
if [[ -f "$BYTES_JAR" ]]; then
  if "$JAVA_HOME/bin/jar" --describe-module --file="$BYTES_JAR" 2>&1 | grep -q "at.favre.lib.bytes@"; then
    NEEDS_BOOTSTRAP=false
  fi
fi
if $NEEDS_BOOTSTRAP; then
  echo ""
  echo "[0/3] Bootstrapping Moditect (one-time per fresh m2 cache)..."
  rm -f "${M2}/at/favre/lib/bcrypt/0.10.2/bcrypt-0.10.2.jar" \
        "${M2}/at/favre/lib/bytes/1.5.0/bytes-1.5.0.jar" \
        "${M2}/org/postgresql/postgresql/42.7.10/postgresql-42.7.10.jar" \
        "${M2}/com/github/librepdf/openpdf/3.0.3/openpdf-3.0.3.jar"
  JAVA_HOME="$JAVA_HOME" ./mvnw -Dmoditect.skip=false generate-resources
else
  echo ""
  echo "[0/3] Moditect bootstrap skipped — m2 cache already patched."
fi

# ── Step 1: Build ─────────────────────────────────────────────────────────────
echo ""
echo "[1/3] Building project..."
JAVA_HOME="$JAVA_HOME" ./mvnw clean package $MVN_FLAGS

# ── Step 2: jlink — runtime image ─────────────────────────────────────────────
echo ""
echo "[2/3] Creating jlink runtime image..."
JAVA_HOME="$JAVA_HOME" ./mvnw javafx:jlink

IMAGE_DIR="target/recetea-runtime"
if [[ -d "$IMAGE_DIR" ]]; then
  SIZE=$(du -sh "$IMAGE_DIR" | cut -f1)
  echo "    Runtime image size: $SIZE"
fi

# ── Step 3: jpackage — portable folder (or installer if --type was overridden) ─
echo ""
echo "[3/3] Packaging (type=$PKG_TYPE)..."
rm -rf target/installer
"$JAVA_HOME/bin/jpackage" \
  --type "$PKG_TYPE" \
  --name Recetea \
  --app-version 1.0.0 \
  --vendor Recetea \
  --runtime-image target/recetea-runtime \
  --module com.recetea/com.recetea.Main \
  --dest target/installer \
  --java-options --enable-preview

echo ""
if [[ "$PKG_TYPE" == "app-image" ]]; then
  APP_DIR="target/installer/Recetea"
  if [[ -d "$APP_DIR" ]]; then
    SIZE=$(du -sh "$APP_DIR" | cut -f1)
    echo "Done. Portable app folder: $APP_DIR ($SIZE)"
    echo "    Layout: Recetea[.exe] + runtime/ + app/Recetea.cfg"
    echo "    Copy this folder anywhere and run the executable — no installation needed."
  else
    echo "Warning: expected $APP_DIR but it was not found." >&2
  fi
else
  echo "Done. Installer written to target/installer/"
  ls -lh target/installer/ 2>/dev/null || true
fi

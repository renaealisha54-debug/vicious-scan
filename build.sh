#!/data/data/com.termux/files/usr/bin/bash
# =============================================================================
#  VICIOUS SCAN — Termux Build Script
#  Builds a debug APK ready to install or push to GitHub Releases.
#
#  Usage (from project root):
#    chmod +x build.sh
#    ./build.sh           # debug APK
#    ./build.sh release   # release APK (requires keystore — see below)
#    ./build.sh clean     # clean build outputs
#    ./build.sh install   # build debug + adb install
# =============================================================================

set -e

# ── Config ──────────────────────────────────────────────────────────────────
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="ViciousScan"
BUILD_TYPE="${1:-debug}"
OUTPUT_DIR="$PROJECT_ROOT/app/build/outputs/apk"
FINAL_DIR="$PROJECT_ROOT/output"

# ── Colors ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

log()  { echo -e "${CYAN}[VS]${RESET} $*"; }
ok()   { echo -e "${GREEN}[OK]${RESET} $*"; }
warn() { echo -e "${YELLOW}[!!]${RESET} $*"; }
err()  { echo -e "${RED}[ERR]${RESET} $*"; exit 1; }

# ── Banner ──────────────────────────────────────────────────────────────────
echo ""
echo -e "${RED}${BOLD}  ██╗   ██╗██╗ ██████╗██╗ ██████╗ ██╗   ██╗███████╗${RESET}"
echo -e "${RED}${BOLD}  ██║   ██║██║██╔════╝██║██╔═══██╗██║   ██║██╔════╝${RESET}"
echo -e "${RED}${BOLD}  ██║   ██║██║██║     ██║██║   ██║██║   ██║███████╗${RESET}"
echo -e "${RED}${BOLD}  ╚██╗ ██╔╝██║██║     ██║██║   ██║██║   ██║╚════██║${RESET}"
echo -e "${RED}${BOLD}   ╚████╔╝ ██║╚██████╗██║╚██████╔╝╚██████╔╝███████║${RESET}"
echo -e "${RED}${BOLD}    ╚═══╝  ╚═╝ ╚═════╝╚═╝ ╚═════╝  ╚═════╝ ╚══════╝${RESET}"
echo -e "  ${BOLD}SCAN${RESET} — build script for Termux / Accio"
echo ""

# ── Clean mode ──────────────────────────────────────────────────────────────
if [ "$BUILD_TYPE" = "clean" ]; then
  log "Cleaning build outputs..."
  cd "$PROJECT_ROOT"
  ./gradlew clean
  rm -rf "$FINAL_DIR"
  ok "Clean complete."
  exit 0
fi

# ── Pre-flight checks ────────────────────────────────────────────────────────
log "Checking environment..."

# Java
if ! command -v java &>/dev/null; then
  warn "Java not found. Installing via pkg..."
  pkg install -y openjdk-17 || err "Failed to install Java. Run: pkg install openjdk-17"
fi
JAVA_VER=$(java -version 2>&1 | head -1)
ok "Java: $JAVA_VER"

# ANDROID_HOME
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
  # Common Termux Android SDK locations
  if [ -d "$HOME/android-sdk" ]; then
    export ANDROID_HOME="$HOME/android-sdk"
  elif [ -d "/storage/emulated/0/Android/sdk" ]; then
    export ANDROID_HOME="/storage/emulated/0/Android/sdk"
  else
    warn "ANDROID_HOME not set and SDK not found at default locations."
    warn "Set it in ~/.bashrc:  export ANDROID_HOME=\$HOME/android-sdk"
    warn "Then run: sdkmanager 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'"
    err "Android SDK required. See README for Termux SDK setup."
  fi
fi
ok "ANDROID_HOME: $ANDROID_HOME"

# gradlew
if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
  err "gradlew not found. Run this script from the project root."
fi
chmod +x "$PROJECT_ROOT/gradlew"
ok "gradlew found."

# ── Build ────────────────────────────────────────────────────────────────────
cd "$PROJECT_ROOT"
mkdir -p "$FINAL_DIR"

if [ "$BUILD_TYPE" = "release" ]; then
  # ── Release build ──
  log "Starting RELEASE build..."

  # Keystore check
  KEYSTORE="${KEYSTORE_PATH:-$PROJECT_ROOT/vicious-scan.keystore}"
  if [ ! -f "$KEYSTORE" ]; then
    warn "No keystore found at $KEYSTORE"
    warn "Generating a new keystore..."
    keytool -genkey -v \
      -keystore "$KEYSTORE" \
      -alias viciousscan \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -storepass viciousscan123 \
      -keypass  viciousscan123 \
      -dname "CN=ViciousScan, OU=Dev, O=Vicious, L=Unknown, S=Unknown, C=US" \
      || err "keytool failed. Install via: pkg install openjdk-17"
    ok "Keystore generated: $KEYSTORE"
    warn "Store/key password: viciousscan123 — change this for production!"
  fi

  export KEYSTORE_FILE="$KEYSTORE"
  export KEY_ALIAS="viciousscan"
  export KEY_PASSWORD="${KEY_PASSWORD:-viciousscan123}"
  export STORE_PASSWORD="${STORE_PASSWORD:-viciousscan123}"

  ./gradlew assembleRelease --no-daemon --stacktrace
  APK_SRC="$OUTPUT_DIR/release/app-release.apk"

else
  # ── Debug build ──
  log "Starting DEBUG build..."
  ./gradlew assembleDebug --no-daemon --stacktrace
  APK_SRC="$OUTPUT_DIR/debug/app-debug.apk"
fi

# ── Copy output ──────────────────────────────────────────────────────────────
if [ -f "$APK_SRC" ]; then
  TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
  APK_DEST="$FINAL_DIR/${APP_NAME}-${BUILD_TYPE}-${TIMESTAMP}.apk"
  cp "$APK_SRC" "$APK_DEST"
  ok "APK ready: $APK_DEST"
  echo ""
  echo -e "  ${BOLD}Size:${RESET} $(du -sh "$APK_DEST" | cut -f1)"
  echo -e "  ${BOLD}Path:${RESET} $APK_DEST"
  echo ""
else
  err "APK not found at expected path: $APK_SRC"
fi

# ── Install mode ─────────────────────────────────────────────────────────────
if [ "$BUILD_TYPE" = "install" ] || [ "$1" = "install" ]; then
  log "Installing via adb..."
  if ! command -v adb &>/dev/null; then
    pkg install -y android-tools || err "adb not found. Run: pkg install android-tools"
  fi
  adb install -r "$APK_DEST" && ok "Installed on device." || warn "adb install failed — is USB debugging on?"
fi

# ── Done ─────────────────────────────────────────────────────────────────────
echo -e "${GREEN}${BOLD}  BUILD COMPLETE${RESET}"
echo ""
echo "  To install manually:"
echo "    adb install -r \"$APK_DEST\""
echo ""
echo "  To push APK to GitHub release:"
echo "    gh release upload v1.0.0 \"$APK_DEST\""
echo ""

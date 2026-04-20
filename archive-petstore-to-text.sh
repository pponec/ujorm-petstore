#!/bin/bash
set -e

# Switch to script directory
cd "$(dirname "$0")"

FILE="$HOME/PetStore.zip"

# Clean old archive
rm -f "$FILE"

# Pack project; exclude VCS, IDE and build artifacts
zip -r "$FILE" . -x ".git/*" ".idea/*" "**/target/*" "target/*"

echo "Stored to $FILE"
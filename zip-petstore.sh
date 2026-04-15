#!/bin/bash
set -e

# Switch to script directory
cd "$(dirname "$0")"

FILE="$HOME/PetStore.zip"

# Clean old archive
rm -f "$FILE"

# Pack project; include hidden files, exclude target folders
zip -r "$FILE" . -x "**/target/*" "target/*"

echo "Stored to $FILE"
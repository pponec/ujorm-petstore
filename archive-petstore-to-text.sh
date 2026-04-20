#!/bin/bash
set -e
cd "$(dirname "$0")"
./archive-petstore-to-zip.sh

# Config:
OUTPUT_FILE="$HOME/PetStore.txt"
ZIP_FILE="$HOME/PetStore.zip"
TEMP_DIR="extract_temp_$(date +%s)"

# 1. Clear old output and create temporary directory
> "$OUTPUT_FILE"
mkdir -p "$TEMP_DIR"

echo "Merging text files from $ZIP_FILE into $OUTPUT_FILE ..."
 
# 2. Extract the ZIP archive
unzip -q "$ZIP_FILE" -d "$TEMP_DIR"

# 3. Traverse files using the grep-I trick
find "$TEMP_DIR" -type f -not -path '*/.*' | while read -r f; do

    # Use grep -I to check if file is text (not binary)
    # -q: quiet, -I: ignore binaries, .: match any character
    if grep -qI . "$f"; then

        # Skip build artifacts
        if [[ "$f" =~ /(target|build)/ ]]; then
            continue
        fi

        # Remove the temporary directory prefix from the path
        RELATIVE_PATH="${f#$TEMP_DIR/}"

        # Write header and file content
        echo "========================================" >> "$OUTPUT_FILE"
        echo "FILE: $RELATIVE_PATH"                     >> "$OUTPUT_FILE"
        echo "========================================" >> "$OUTPUT_FILE"
        cat "$f"     >> "$OUTPUT_FILE"
        echo -e "\n" >> "$OUTPUT_FILE"
    fi
done

# 4. Cleanup
rm -rf "$TEMP_DIR"

echo "------------------------------------------------"
echo "DONE! Text files merged into: $OUTPUT_FILE"

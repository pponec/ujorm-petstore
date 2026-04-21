#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "-----------------------------------------------------"
echo "Setting up Python PetStore environment..."
echo "-----------------------------------------------------"

# Check if pip is available
if ! command -v pip3 &> /dev/null
then
    echo "pip3 could not be found, attempting to use pip..."
    PIP_CMD="pip"
else
    PIP_CMD="pip3"
fi

# Install dependencies
echo "Installing dependencies..."
python3 -m pip install --break-system-packages -r requirements.txt || pip install -r requirements.txt || echo "Warning: Pip failed"

echo ""
echo "====================================================="
echo "Starting the application via Uvicorn..."
echo "The PetStore will be available at:"
echo ""
echo "   http://localhost:3000/"
echo ""
echo "====================================================="
echo "Press Ctrl+C to stop the application."
echo ""

# Run the application
python3 main.py || python main.py

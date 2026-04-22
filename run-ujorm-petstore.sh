#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e
cd "$(dirname "$0")"

echo "Compiling Ujorm PetStore using Maven Wrapper..."
./mvnw clean compile

echo "Compilation successful."
echo ""
echo "====================================================="
echo "Starting the application via Jetty Maven Plugin..."
echo "Once Jetty finishes initialization,"
echo "the PetStore will be available at:"
echo ""
echo "   http://localhost:3000/"
echo ""
echo "====================================================="
echo "Press Ctrl+C to stop the application."
echo ""

# Run database PostgreSql
./run-database.sh

# Run the application using the Jetty plugin
./mvnw jetty:run

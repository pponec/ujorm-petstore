#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e
cd "$(dirname "$0")"

# The port of the jetty-ee10-maven-plugin, so that this text cannot drift from the build
PORT=$(sed -n 's|.*<port>\([0-9]\+\)</port>.*|\1|p' pom.xml | head -1)
PORT=${PORT:-8080}

echo "Compiling Ujorm PetStore using Maven Wrapper..."
./mvnw clean compile

echo "Compilation successful."
echo ""
echo "====================================================="
echo "Starting the application via Jetty Maven Plugin..."
echo "Once Jetty finishes initialization,"
echo "the PetStore will be available at:"
echo ""
echo "   http://localhost:${PORT}/"
echo ""
echo "====================================================="
echo "Press Ctrl+C to stop the application."
echo ""

# Run the application using the Jetty plugin
./mvnw jetty:run

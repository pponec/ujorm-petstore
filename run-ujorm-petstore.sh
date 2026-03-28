#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Compiling Ujorm PetStore using Maven Wrapper..."
./mvnw clean compile

echo "Compilation successful."
echo ""
echo "====================================================="
echo "Starting the application..."
echo "Once Spring Boot finishes initialization,"
echo "the PetStore will be available at:"
echo ""
echo "   http://localhost:8080/"
echo ""
echo "====================================================="
echo "Press Ctrl+C to stop the application."
echo ""

# Run the Spring Boot application
./mvnw spring-boot:run
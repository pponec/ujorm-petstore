#!/bin/bash

# ==============================================================================
# PostgreSQL Database Container Management Script
#
# Required Environment Variables:
#   APP_DB_NAME           - Name of the database to create (default 'demo')
#   APP_DB_USER           - Username used by both App and DB Admin
#   APP_DB_PASSWORD       - Password used by both App and DB Admin
#
# Usage:
#   ./run-database.sh          - Starts the container (builds image if missing)
#   ./run-database.sh stop     - Stops the running container
#   ./run-database.sh restart  - Restarts the container
#   ./run-database.sh delete   - Stops and removes the container
#   docker logs -f postgres-demo - Show logs
# ==============================================================================

# Exit immediately if a command exits with a non-zero status
set -e
cd "$(dirname "$0")"

# Static constants
CONTAINER_NAME="postgres-demo"
IMAGE_NAME="my-postgres-alpine"

# Using shell parameter expansion for default value
DB_NAME="${APP_DB_NAME:-demo}"

# Function to check if the container exists
container_exists() {
  docker ps -a --format '{{.Names}}' | grep -Eq "^${CONTAINER_NAME}$"
}

# Function to check if the container is running
is_running() {
  docker ps --format '{{.Names}}' | grep -Eq "^${CONTAINER_NAME}$"
}

# Function to check if the image exists locally
image_exists() {
  docker images -q "$IMAGE_NAME" | grep -q .
}

# Check if Docker is installed, running, and accessible by the current user
if ! docker info >/dev/null 2>&1; then
    echo "Error: Docker is either not installed, not running, or the current user lacks permissions." >&2
    exit 1
fi

case "$1" in
  stop)
    echo "Stopping container $CONTAINER_NAME..."
    docker stop "$CONTAINER_NAME" 2>/dev/null || echo "Container was not running."
    ;;

  restart)
    echo "Restarting container $CONTAINER_NAME..."
    docker restart "$CONTAINER_NAME"
    ;;

  delete)
    echo "Stopping and removing container $CONTAINER_NAME..."
    docker stop "$CONTAINER_NAME" 2>/dev/null || true
    docker rm "$CONTAINER_NAME" 2>/dev/null || true
    echo "Container deleted. Data in volume remains persistent."
    ;;

  "")
    # Default: Start logic
    if is_running; then
      echo "Container '$CONTAINER_NAME' is already running (DB: $DB_NAME)."
    elif container_exists; then
      echo "Container exists but is stopped. Starting now..."
      docker start "$CONTAINER_NAME"
    else
      # Check if the Docker image needs to be built
      if ! image_exists; then
        echo "Image '$IMAGE_NAME' not found locally. Building from Dockerfile..."
        docker build -t "$IMAGE_NAME" .
      fi

      echo "Creating and starting a new container '$CONTAINER_NAME' with database '$DB_NAME'..."

      # Validate mandatory environment variables before run
      if [ -z "$APP_DB_USER" ] || [ -z "$APP_DB_PASSWORD" ]; then
        echo "Error: variables APP_DB_USER and APP_DB_PASSWORD are required."
        exit 1
      fi

      docker run -d \
        --name "$CONTAINER_NAME" \
        --memory="768m" \
        --restart unless-stopped \
        -p 5432:5432 \
        -e POSTGRES_DB="$DB_NAME" \
        -e POSTGRES_USER="$APP_DB_USER" \
        -e POSTGRES_PASSWORD="$APP_DB_PASSWORD" \
        -v pg_data_demo:/var/lib/postgresql \
        "$IMAGE_NAME"
    fi
    ;;

  *)
    echo "Invalid parameter: $1"
    echo "Usage: $0 {stop|restart|delete| (no parameter to start)}"
    exit 1
    ;;
esac
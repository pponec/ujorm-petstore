#!/bin/bash
#
# Builds the Ujorm PetStore into a single native Linux executable by GraalVM and starts it.
# The compiler runs inside a Docker image, so no GraalVM is installed on the host.
#
# Usage:
#   ./run-ujorm-petstore-native.sh               build the binary and start it
#   ./run-ujorm-petstore-native.sh --build-only  build the binary without starting it
#
# See run-ujorm-petstore.sh to run the application on a JVM in the Jetty plugin instead.

# Exit immediately if a command exits with a non-zero status
set -e
cd "$(dirname "$0")"

# The GraalVM distribution used for the build. It is pulled on the first run only.
GRAALVM_IMAGE="ghcr.io/graalvm/native-image-community:25"

# The port announced below and passed to the binary, taken from the jetty-ee10-maven-plugin,
# so that this script cannot drift from the build
PORT=$(sed -n 's|.*<port>\([0-9]\+\)</port>.*|\1|p' pom.xml | head -1)
PORT=${PORT:-8080}

BINARY="target/ujorm-petstore"
MAIN_CLASS="org.ujorm.petstore.Main"

# The arguments are validated before anything is built, so a typo costs no build time.
BUILD_ONLY=false
case "$1" in
    "")           ;;
    --build-only) BUILD_ONLY=true ;;
    *)            echo "Unknown option: $1 (only --build-only is accepted)" >&2; exit 1 ;;
esac

if ! command -v docker > /dev/null; then
    echo "Docker is required for the native build, but it was not found." >&2
    exit 1
fi

# The package phase pre-compiles the Ujorm domain handlers and copies the dependencies
# to target/deps, which together form the class path of the native build below.
echo "Compiling Ujorm PetStore using Maven Wrapper..."
./mvnw clean package

echo ""
echo "Building the native binary by GraalVM in Docker (a few minutes on the first run)..."
echo "Image: ${GRAALVM_IMAGE}"

# The container runs under the current user, so the artifacts do not end up owned by root.
# The reflection and resource metadata are read from META-INF/native-image on the class path.
# The one exception is the config bundled in the H2 jar: it registers java.awt.Desktop and
# would drag the whole java.desktop module in, so it is excluded and replaced by a filtered
# copy in META-INF/native-image/org.ujorm.petstore/h2-without-awt.
docker run --rm \
    --user "$(id -u):$(id -g)" \
    --env HOME=/tmp \
    --volume "$PWD:/project" \
    --workdir /project \
    --entrypoint native-image \
    "${GRAALVM_IMAGE}" \
    -classpath "target/classes:target/deps/*" \
    -o "${BINARY}" \
    --no-fallback \
    --exclude-config 'h2-.*\.jar' 'META-INF/native-image/reflect-config\.json' \
    -H:+ReportExceptionStackTraces \
    "${MAIN_CLASS}"

echo ""
echo "Binary: $(du -h "${BINARY}" | cut -f1)  ${BINARY}"

if [ "${BUILD_ONLY}" = true ]; then
    exit 0
fi

echo ""
echo "====================================================="
echo "Starting the native executable."
echo "Once the initialization finishes,"
echo "the PetStore will be available at:"
echo ""
echo "   http://localhost:${PORT}/"
echo ""
echo "====================================================="
echo "Press Ctrl+C to stop the application."
echo ""

exec "./${BINARY}" "${PORT}"

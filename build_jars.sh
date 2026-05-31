#!/bin/bash

# Exit on error
set -e

echo "=========================================================="
echo "Starting CloudBid Project Packaging..."
echo "=========================================================="

# Build the Maven project skipping tests
./mvnw clean package -DskipTests

echo ""
echo "=========================================================="
echo "Maven build completed successfully!"
echo "Copying executable JAR files to root directory..."
echo "=========================================================="

# Copy the server executable JAR
if [ -f server/target/server-0.0.1-SNAPSHOT.jar ]; then
    cp server/target/server-0.0.1-SNAPSHOT.jar ./server.jar
    echo "✔ Copied server.jar to root"
else
    echo "✘ Error: Server JAR not found!"
    exit 1
fi

# Copy the client shaded JAR
if [ -f client/target/client-0.0.1-SNAPSHOT.jar ]; then
    cp client/target/client-0.0.1-SNAPSHOT.jar ./client.jar
    echo "✔ Copied client.jar to root"
else
    echo "✘ Error: Client JAR not found!"
    exit 1
fi

echo "=========================================================="
echo "Packaging complete!"
echo "You can now run:"
echo "  1. Server: java -jar server.jar"
echo "  2. Client: java -jar client.jar"
echo "=========================================================="

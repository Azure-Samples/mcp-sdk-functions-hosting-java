#!/bin/bash

echo "Running on Port ${FUNCTIONS_CUSTOMHANDLER_PORT}"
export JAVA_HOME=/usr/lib/jvm/msft-17-x64
export PATH="${JAVA_HOME}/bin:${PATH}"

export MCP_SERVER_PORT=${FUNCTIONS_CUSTOMHANDLER_PORT}

# Find the single JAR file in the current directory
JAR_FILE=$(ls *.jar 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "Error: No JAR file found in the current directory"
    exit 1
fi

echo "Starting MCP server with JAR: $JAR_FILE"
java -jar "$JAR_FILE"
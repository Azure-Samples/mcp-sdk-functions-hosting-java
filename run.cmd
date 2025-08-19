@echo off
echo Running on Port %FUNCTIONS_CUSTOMHANDLER_PORT%

REM Set the port for Spring Boot
set PORT=%FUNCTIONS_CUSTOMHANDLER_PORT%

REM Run the Java application
java -jar mcp-1.0.jar

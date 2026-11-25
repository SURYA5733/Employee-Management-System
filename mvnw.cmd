@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script for Windows
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_CMD_ECHO__%"=="" @ECHO off
SETLOCAL EnableExtensions EnableDelayedExpansion

SET "DIR=%~dp0"
IF "%DIR:~-1%"=="\" SET "DIR=%DIR:~0,-1%"

@REM Try system PATH maven first
WHERE mvn.exe >nul 2>nul
IF %ERRORLEVEL% EQU 0 (
    mvn.exe %*
    EXIT /B %ERRORLEVEL%
)

@REM Fallback to cached Maven distribution if available
SET "CACHED_MVN=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd"
IF EXIST "%CACHED_MVN%" (
    CALL "%CACHED_MVN%" %*
    EXIT /B %ERRORLEVEL%
)

SET "CACHED_MVN2=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd"
IF EXIST "%CACHED_MVN2%" (
    CALL "%CACHED_MVN2%" %*
    EXIT /B %ERRORLEVEL%
)

ECHO Error: Apache Maven was not found in PATH or .m2 directory.
ECHO Please install Maven or add it to PATH.
EXIT /B 1

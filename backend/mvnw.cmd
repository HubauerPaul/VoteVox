@REM ----------------------------------------------------------------------------
@REM VoteVox Maven Wrapper — Windows batch script
@REM Downloads Apache Maven on first run, then delegates to mvn.cmd.
@REM ----------------------------------------------------------------------------
@echo off
setlocal enabledelayedexpansion

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_PROPERTIES%" (
  echo Cannot find %WRAPPER_PROPERTIES% 1>&2
  exit /b 1
)

@REM Parse distributionUrl from maven-wrapper.properties.
set DISTRIBUTION_URL=
for /f "usebackq tokens=1,2 delims==" %%A in ("%WRAPPER_PROPERTIES%") do (
  if "%%A"=="distributionUrl" set DISTRIBUTION_URL=%%B
)

if "%DISTRIBUTION_URL%"=="" (
  echo distributionUrl not found in %WRAPPER_PROPERTIES% 1>&2
  exit /b 1
)

@REM Derive the distribution name, e.g. apache-maven-3.9.9.
for %%F in ("%DISTRIBUTION_URL%") do set DIST_FILE=%%~nxF
for %%F in ("%DIST_FILE%") do set DIST_BASE=%%~nF
set DIST_NAME=%DIST_BASE:-bin=%

if "%MAVEN_USER_HOME%"=="" set MAVEN_USER_HOME=%USERPROFILE%\.m2
set MAVEN_HOME=%MAVEN_USER_HOME%\wrapper\dists\%DIST_NAME%

@REM Look for an already-extracted distribution.
set MVN_EXE=
if exist "%MAVEN_HOME%" (
  for /f "delims=" %%D in ('dir /b /ad "%MAVEN_HOME%" 2^>nul') do (
    if exist "%MAVEN_HOME%\%%D\bin\mvn.cmd" set MVN_EXE=%MAVEN_HOME%\%%D\bin\mvn.cmd
  )
)

if "%MVN_EXE%"=="" (
  echo Downloading Maven distribution from %DISTRIBUTION_URL% ...
  if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"
  set "TMP_ZIP=%MAVEN_HOME%\%DIST_FILE%"

  powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%MAVEN_HOME%\%DIST_FILE%' -UseBasicParsing"
  if errorlevel 1 (
    echo Failed to download Maven from %DISTRIBUTION_URL% 1>&2
    exit /b 1
  )

  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%MAVEN_HOME%\%DIST_FILE%' -DestinationPath '%MAVEN_HOME%' -Force"
  if errorlevel 1 (
    echo Failed to extract Maven 1>&2
    exit /b 1
  )

  del "%MAVEN_HOME%\%DIST_FILE%" >nul 2>&1

  for /f "delims=" %%D in ('dir /b /ad "%MAVEN_HOME%"') do (
    if exist "%MAVEN_HOME%\%%D\bin\mvn.cmd" set MVN_EXE=%MAVEN_HOME%\%%D\bin\mvn.cmd
  )
)

if "%MVN_EXE%"=="" (
  echo Could not locate mvn.cmd in %MAVEN_HOME% 1>&2
  exit /b 1
)

call "%MVN_EXE%" %*
exit /b %ERRORLEVEL%

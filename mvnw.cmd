@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM   https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM   MAVEN_BATCH_ECHO  - set to 'on' to enable echoing of batch commands
@REM   MAVEN_BATCH_PAUSE - set to 'on' to wait for keystroke before ending
@REM   MAVEN_OPTS        - parameters passed to the Java VM when running Maven
@REM   MVNW_USERNAME     - username for downloading wrapper jar (proxy/private repo)
@REM   MVNW_PASSWORD     - password for downloading wrapper jar
@REM ----------------------------------------------------------------------------

@IF "%MAVEN_BATCH_ECHO%"=="on" echo %MAVEN_BATCH_ECHO%

@REM Set %HOME% to equivalent of $HOME
IF "%HOME%"=="" (SET "HOME=%HOMEDRIVE%%HOMEPATH%")

SET ERROR_CODE=0

@setlocal

SET "PROJECT_BASEDIR=%~dp0"
IF "%PROJECT_BASEDIR:~-1%"=="\" SET "PROJECT_BASEDIR=%PROJECT_BASEDIR:~0,-1%"

SET "MAVEN_WRAPPER_JAR=%PROJECT_BASEDIR%\.mvn\wrapper\maven-wrapper.jar"
SET "MAVEN_WRAPPER_PROPERTIES=%PROJECT_BASEDIR%\.mvn\wrapper\maven-wrapper.properties"

@REM ==== Localizar Java ====
IF NOT "%JAVA_HOME%"=="" (
  IF EXIST "%JAVA_HOME%\bin\java.exe" (
    SET "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    GOTO OkJHome
  )
  echo.
  echo Erro: JAVA_HOME aponta para um diretório inválido: "%JAVA_HOME%" >&2
  echo Ajuste a variável de ambiente JAVA_HOME. >&2
  echo.
  SET ERROR_CODE=1
  GOTO end
)

FOR /f "tokens=*" %%i IN ('where java 2^>nul') DO SET "JAVA_CMD=%%i"

IF "%JAVA_CMD%"=="" (
  echo.
  echo Erro: JAVA_HOME não definido e 'java' não encontrado no PATH. >&2
  echo Instale o JDK 25 e defina JAVA_HOME. >&2
  echo.
  SET ERROR_CODE=1
  GOTO end
)

:OkJHome

@REM ==== Baixar maven-wrapper.jar se não existir ====
IF NOT EXIST "%MAVEN_WRAPPER_JAR%" (
  echo Baixando maven-wrapper.jar...

  SET "WRAPPER_URL="
  FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_WRAPPER_PROPERTIES%") DO (
    IF "%%A"=="wrapperUrl" SET "WRAPPER_URL=%%B"
  )

  IF "%WRAPPER_URL%"=="" (
    SET "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  )

  powershell -Command ^
    "$wc = New-Object System.Net.WebClient;" ^
    "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%'))){ $wc.Credentials = New-Object System.Net.NetworkCredential('%MVNW_USERNAME%','%MVNW_PASSWORD%'); }" ^
    "$wc.DownloadFile('%WRAPPER_URL%', '%MAVEN_WRAPPER_JAR%');"

  IF NOT EXIST "%MAVEN_WRAPPER_JAR%" (
    echo Falha ao baixar com PowerShell. Tentando com curl...
    curl -fsSL -o "%MAVEN_WRAPPER_JAR%" "%WRAPPER_URL%"
  )

  IF NOT EXIST "%MAVEN_WRAPPER_JAR%" (
    echo.
    echo Erro: Não foi possível baixar o maven-wrapper.jar. >&2
    echo Verifique a conectividade com: https://repo.maven.apache.org >&2
    echo.
    SET ERROR_CODE=1
    GOTO end
  )
)

@REM ==== Executar Maven via wrapper ====
"%JAVA_CMD%" ^
  %MAVEN_OPTS% ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%PROJECT_BASEDIR%" ^
  "-Dmaven.wrapper.properties.file=%MAVEN_WRAPPER_PROPERTIES%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

SET ERROR_CODE=%ERRORLEVEL%

@endlocal & SET ERROR_CODE=%ERROR_CODE%

:end
IF "%MAVEN_BATCH_PAUSE%"=="on" PAUSE
EXIT /B %ERROR_CODE%

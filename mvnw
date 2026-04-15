#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup shell script, version 3.3.2
#
# Required ENV vars:
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV vars
#   MAVEN_OPTS        - parameters passed to the Java VM when running Maven
#   MVNW_USERNAME     - username for downloading wrapper jar
#   MVNW_PASSWORD     - password for downloading wrapper jar
# ----------------------------------------------------------------------------

set -e

# Resolve base directory (follow symlinks)
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done
PRGDIR=$(dirname "$PRG")
PROJECT_BASEDIR=$(cd "$PRGDIR" || exit 1; pwd)

MAVEN_WRAPPER_JAR="$PROJECT_BASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$PROJECT_BASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Localizar Java
if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
  else
    echo "Erro: JAVA_HOME não aponta para um JDK válido: $JAVA_HOME" >&2
    exit 1
  fi
else
  JAVA_CMD=$(command -v java 2>/dev/null)
  if [ -z "$JAVA_CMD" ]; then
    echo "Erro: JAVA_HOME não definido e 'java' não encontrado no PATH." >&2
    echo "Instale o JDK 25 e defina JAVA_HOME." >&2
    exit 1
  fi
fi

# Baixar maven-wrapper.jar se não existir
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
  WRAPPER_URL=$(grep '^wrapperUrl=' "$MAVEN_WRAPPER_PROPERTIES" 2>/dev/null | cut -d'=' -f2-)
  if [ -z "$WRAPPER_URL" ]; then
    WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  fi

  echo "Baixando maven-wrapper.jar de $WRAPPER_URL..."

  if command -v curl > /dev/null 2>&1; then
    if [ -n "$MVNW_USERNAME" ]; then
      curl -fsSL -u "$MVNW_USERNAME:$MVNW_PASSWORD" -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    else
      curl -fsSL -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    fi
  elif command -v wget > /dev/null 2>&1; then
    if [ -n "$MVNW_USERNAME" ]; then
      wget -q --http-user="$MVNW_USERNAME" --http-password="$MVNW_PASSWORD" -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    else
      wget -q -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    fi
  else
    echo "Erro: curl ou wget não encontrado. Instale um deles." >&2
    exit 1
  fi

  if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Erro: falha ao baixar $WRAPPER_URL" >&2
    exit 1
  fi
fi

# Executar Maven via wrapper
exec "$JAVA_CMD" \
  $MAVEN_OPTS \
  -classpath "$MAVEN_WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$PROJECT_BASEDIR" \
  "-Dmaven.wrapper.properties.file=$MAVEN_WRAPPER_PROPERTIES" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"

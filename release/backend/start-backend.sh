#!/bin/bash

set -e

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_JAR="$BASE_DIR/language-exchange-platform-0.0.1-SNAPSHOT.jar"
APP_CONFIG="$BASE_DIR/application-prod.yml"
APP_LOG="$BASE_DIR/backend.log"

if [ ! -f "$APP_JAR" ]; then
  echo "Jar not found: $APP_JAR"
  exit 1
fi

nohup java -Xms256m -Xmx768m -Dfile.encoding=UTF-8 -jar "$APP_JAR" \
  --spring.config.location="file:$APP_CONFIG" \
  > "$APP_LOG" 2>&1 &

echo "Backend started. Log: $APP_LOG"

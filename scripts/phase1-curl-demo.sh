#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${EMAIL:-admin@demo.com}"
PASSWORD="${PASSWORD:-admin123}"

echo "[1/6] Login"
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
  echo "Failed to retrieve JWT token"
  exit 1
fi

echo "[2/6] Auth me"
curl -s "$BASE_URL/api/auth/me" \
  -H "Authorization: Bearer $TOKEN"
echo

echo "[3/6] Create project"
PROJECT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/projects" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Phase1 Project","description":"Phase 1 demo project","projectKey":"PH1"}')

echo "$PROJECT_RESPONSE"
PROJECT_ID=$(echo "$PROJECT_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')

if [ -z "$PROJECT_ID" ]; then
  echo "Failed to create project"
  exit 1
fi

echo "[4/6] List projects"
curl -s "$BASE_URL/api/projects" \
  -H "Authorization: Bearer $TOKEN"
echo

echo "[5/6] Create task in project $PROJECT_ID"
TASK_RESPONSE=$(curl -s -X POST "$BASE_URL/api/projects/$PROJECT_ID/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Phase1 Task","description":"Phase 1 task demo","priority":"HIGH","estimatedHours":4}')

echo "$TASK_RESPONSE"
TASK_ID=$(echo "$TASK_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')

if [ -z "$TASK_ID" ]; then
  echo "Failed to create task"
  exit 1
fi

echo "[6/6] Update task status"
curl -s -X PATCH "$BASE_URL/api/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS","actualHours":2}'
echo

echo "Done. Project ID=$PROJECT_ID Task ID=$TASK_ID"

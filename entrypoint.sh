#!/usr/bin/env bash
set -euo pipefail

# Start FastAPI backend on port 8000
cd /app/python
python3 -m uvicorn main:app --host 0.0.0.0 --port 8000 &
FASTAPI_PID=$!
cd /app

# Start Streamlit UI on port 8501
python3 -m streamlit run ./python/ui_app.py --server.port 8501 --server.address 0.0.0.0 &
STREAMLIT_PID=$!

# Start Java Spring Boot on port 8080
java -jar app.jar &
JAVA_PID=$!

# Forward signals to all children
_graceful_shutdown() {
  kill -TERM "$FASTAPI_PID" "$STREAMLIT_PID" "$JAVA_PID" 2>/dev/null || true
}
trap _graceful_shutdown INT TERM

EXIT_STATUS=0

# Wait for any process to exit
if wait -n "$FASTAPI_PID" "$STREAMLIT_PID" "$JAVA_PID" 2>/dev/null; then
  EXIT_STATUS=$?
else
  # Fallback: poll which PID died first
  while true; do
    if ! kill -0 "$FASTAPI_PID" 2>/dev/null; then
      if wait "$FASTAPI_PID" 2>/dev/null; then EXIT_STATUS=0; else EXIT_STATUS=$?; fi
      break
    fi
    if ! kill -0 "$STREAMLIT_PID" 2>/dev/null; then
      if wait "$STREAMLIT_PID" 2>/dev/null; then EXIT_STATUS=0; else EXIT_STATUS=$?; fi
      break
    fi
    if ! kill -0 "$JAVA_PID" 2>/dev/null; then
      if wait "$JAVA_PID" 2>/dev/null; then EXIT_STATUS=0; else EXIT_STATUS=$?; fi
      break
    fi
    sleep 0.5
  done
fi

# Terminate remaining processes
_graceful_shutdown
wait "$FASTAPI_PID" "$STREAMLIT_PID" "$JAVA_PID" 2>/dev/null || true

exit "$EXIT_STATUS"
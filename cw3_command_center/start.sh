#!/bin/bash
# Startup script for MedSupplyDrones Command Center
# Starts both the FastAPI backend and Streamlit UI

set -e

echo "========================================="
echo "MedSupplyDrones Command Center"
echo "========================================="

# Check if .env exists
if [ ! -f .env ]; then
    echo "Warning: .env file not found. Copying from .env.example"
    cp .env.example .env
    echo "Please edit .env and set your GEMINI_API_KEY before running again"
    exit 1
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Check required environment variables
if [ -z "$GEMINI_API_KEY" ]; then
    echo "Error: GEMINI_API_KEY not set in .env"
    exit 1
fi

echo ""
echo "Configuration:"
echo "  ILP Endpoint: ${ILPENDPOINT:-https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net}"
echo "  CW2 Endpoint: ${CW2_ENDPOINT:-http://localhost:8080}"
echo "  Gemini Model: ${GEMINI_MODEL:-gemini-2.5-flash}"
echo ""

# Install dependencies if needed
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

echo "Activating virtual environment..."
source venv/bin/activate

echo "Installing dependencies..."
pip install -q --upgrade pip
pip install -q -r requirements.txt

echo ""
echo "Starting services..."
echo ""

# Start FastAPI in background
echo "Starting Dispatch Engine API on http://localhost:8000"
uvicorn main:app --host 0.0.0.0 --port 8000 --reload &
API_PID=$!

# Wait a bit for API to start
sleep 3

# Set API URL for Streamlit
export DISPATCH_API_URL="http://localhost:8000"

# Start Streamlit
echo "Starting Command Center UI on http://localhost:8501"
echo ""
echo "========================================="
echo "Services running:"
echo "  - API: http://localhost:8000"
echo "  - UI:  http://localhost:8501"
echo "========================================="
echo ""
streamlit run ui_app.py --server.port=8501 --server.address=0.0.0.0

# Cleanup on exit
trap "kill $API_PID" EXIT

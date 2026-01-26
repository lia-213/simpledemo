# ILP CW3: MedSupplyDrones Command Center

**Natural-language Command Center for MedSupplyDrones** — LLM-driven dispatch planner and what-if simulator built on top of ILP REST and CW2 services.

---

## What's Included

This repository contains:

- **`src/`** — Java source code for CW2 REST service (Spring Boot)
- **`cw3_command_center/`** — Python-based natural language interface (FastAPI + Streamlit + Gemini LLM)
- **`s2141930_complete.tar`** — Docker image containing the complete CW2 + CW3 integrated service (**required for marking**)
- **`cw3_explanation.pdf`** — CW3 submission document (max 1,000 words) explaining innovation, benefit, and implementation
- **`pom.xml`** — Maven project configuration
- **`Dockerfile`** — Container build definition
- **`.env.example`** — Example environment variable configuration

---

## Artifact for Marking

**File name**: `s2141930_complete.tar`  
**Location**: Project root directory  
**Contents**: Complete Docker image with CW2 Java REST service + CW3 Python command center

This tar file contains the runnable Docker image required for CW3 evaluation.

---

## Environment Variables

The following environment variables are required:

| Variable | Description | Example |
|----------|-------------|---------|
| `ILP_ENDPOINT` | ILP REST service URL | `https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net` |
| `GEMINI_API_KEY` | Google Gemini API key for natural language processing | `AIzaSy...` (get free key at https://aistudio.google.com/app/apikey) |
| `CW2_ENDPOINT` | Internal CW2 service endpoint | `http://localhost:8080` |

Set these in a `.env` file (see `.env.example` in `cw3_command_center/` directory).

---

## Quick Start

### Option 1: Load Provided Tar (Recommended for Markers)

```powershell
# Navigate to project root
cd c:\path\to\simpledemo

# Load the Docker image
docker load -i s2141930_complete.tar

# Verify image loaded
docker images

# Run the container (all services on ports 8080, 8000, 8501)
docker run -d --name medsupply-demo `
  -e ILP_ENDPOINT=https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net `
  -e GEMINI_API_KEY=your_gemini_key_here `
  -p 8080:8080 -p 8000:8000 -p 8501:8501 `
  s2141930_complete:latest

# Verify CW2 service is running (should return student ID)
curl http://localhost:8080/api/v1/uid
# Or in PowerShell: Invoke-WebRequest -Uri http://localhost:8080/api/v1/uid
```

### Option 2: Build Locally and Create Tar

If you want to rebuild from source and produce the tar:

```powershell
# Navigate to project root
cd c:\path\to\simpledemo

# Build Docker image
docker build -t simpledemo:local .

# Run container with environment file
docker run -d --name simpledemo `
  --env-file cw3_command_center\.env `
  -p 8080:8080 -p 8000:8000 -p 8501:8501 `
  simpledemo:local

# Test the service
curl http://localhost:8080/api/v1/uid

# Tag for distribution (optional - use your student ID)
docker tag simpledemo:local s2141930_complete:latest

# Save to tar file for submission
docker save s2141930_complete:latest -o s2141930_complete.tar
```

### Verify Installation

```powershell
# Check loaded images
docker images | Select-String "s2141930_complete"

# Check running containers
docker ps

# Test CW2 REST endpoints
curl http://localhost:8080/api/v1/uid
curl http://localhost:8080/api/v1/drones

# Test CW3 FastAPI (dispatch engine)
curl http://localhost:8000/summary/fleet

# Access CW3 UI in browser
# Open: http://localhost:8501
```

---

## Running Locally (Development)

For local development without Docker:

### CW2 Service (Java/Spring Boot)
```powershell
# Set environment variable
$env:ILP_ENDPOINT="https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net"

# Run with Maven
mvn spring-boot:run
```

### CW3 Services (Python)
```powershell
cd cw3_command_center

# Create virtual environment
python -m venv venv
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Configure environment
copy .env.example .env
# Edit .env and set GEMINI_API_KEY

# Start FastAPI (Terminal 1)
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# Start Streamlit UI (Terminal 2)
streamlit run ui_app.py --server.port=8501
```

---

## API Documentation and Tests

Once the container is running:

- **CW2 REST API**: http://localhost:8080/api/v1/
  - UID endpoint: http://localhost:8080/api/v1/uid
  - Drones: http://localhost:8080/api/v1/drones
  - Service Points: http://localhost:8080/api/v1/service-points
  
- **CW3 Dispatch API (FastAPI)**: http://localhost:8000/docs
  - Interactive Swagger UI with all endpoints
  - OpenAPI spec: http://localhost:8000/openapi.json

- **CW3 Command Center UI (Streamlit) - core CW3 project**: http://localhost:8501
  - Natural language planning interface
  - Fleet query mode
  - What-if analysis

### Running Tests

```powershell
# CW2 Java tests
mvn test

# CW3 Python tests
cd cw3_command_center
pytest
```

---

## Troubleshooting

### Ports Already in Use
```powershell
# Check what's using ports
netstat -ano | findstr ":8080"
netstat -ano | findstr ":8000"
netstat -ano | findstr ":8501"

# Stop existing container
docker stop medsupply-demo
docker rm medsupply-demo
```

### Docker Daemon Not Running
- Ensure Docker Desktop is running (Windows/Mac)
- Or start Docker service (Linux): `sudo systemctl start docker`

### Insufficient Disk Space
- Docker images can be large (1-2GB)
- Check available space: `docker system df`
- Clean up: `docker system prune -a` (warning: removes unused images)

### Image Load Fails
```powershell
# Verify tar file integrity
dir s2141930_complete.tar

# Check tar is valid
docker load -i s2141930_complete.tar
# Look for "Loaded image" message with image name/tag

# If image has different tag, retag it:
docker tag <IMAGE_ID> s2141930_complete:latest
```

### GEMINI_API_KEY Not Set
- Get free API key: https://aistudio.google.com/app/apikey
- Add to `.env` file in `cw3_command_center/` directory
- Or pass as environment variable when running container

### Connection Refused Errors
```powershell
# Check all services are running
docker ps

# View container logs
docker logs medsupply-demo

# Test individual services
curl http://localhost:8080/api/v1/uid
curl http://localhost:8000/summary/fleet
```

---

## Project Structure

```
simpledemo/
├── s2141930_complete.tar          # Docker image (for marking)
├── cw3_explanation.pdf             # CW3 submission document
├── README.md                       # This file
├── pom.xml                         # Maven configuration
├── Dockerfile                      # Multi-service container build
├── src/                            # CW2 Java source code
│   └── main/java/...
├── cw3_command_center/             # CW3 Python services
│   ├── main.py                     # FastAPI dispatch engine
│   ├── ui_app.py                   # Streamlit UI
│   ├── llm_parser.py               # Gemini LLM integration
│   ├── planner.py                  # Route planning logic
│   ├── requirements.txt            # Python dependencies
│   ├── .env.example                # Environment template
│   └── markdowns and demos/
│       ├── README.md               # Detailed CW3 documentation
│       └── QUICKSTART.md           # Step-by-step setup guide
└── data/
    └── geojsons/                   # Generated flight plans
```

---

## CW3 Innovation Summary

This project provides a **natural language interface** for the MedSupplyDrones dispatch system, addressing the gap between technical REST APIs and business-level operations:

- **Natural Language Planning**: Describe deliveries in plain English, automatically parsed into dispatch requests
- **LLM Integration**: Gemini-powered intent parser converts user text to structured JSON
- **What-If Analysis**: Compare planning strategies (min cost vs min moves) side-by-side
- **Fleet Queries**: Ask capability questions in natural language ("How many drones can handle cooled deliveries?")
- **Visual Routes**: GeoJSON flight path generation for map visualization

**Tech Stack**: Java (Spring Boot) + Python (FastAPI + Streamlit) + Google Gemini LLM + Docker

For detailed documentation, see:
- `cw3_command_center/markdowns and demos/README.md` — Full feature documentation
- `cw3_command_center/markdowns and demos/QUICKSTART.md` — Setup walkthrough
- `cw3_explanation.pdf` — Official submission document

---

## Support

For detailed setup instructions, troubleshooting, and usage examples:
- API documentation: http://localhost:8000/docs (when running)
- UI walkthrough: http://localhost:8501 (when running)

---

**Student ID**: s2141930  
**Submission**: CW3 - ILP Coursework 2025  
**Repository**: https://github.com/lia-213/ilp_cw1_lk

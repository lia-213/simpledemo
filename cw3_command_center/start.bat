@echo off
REM Startup script for MedSupplyDrones Command Center (Windows)
REM Starts both the FastAPI backend and Streamlit UI

echo =========================================
echo MedSupplyDrones Command Center
echo =========================================

REM Check if .env exists
if not exist .env (
    echo Warning: .env file not found. Copying from .env.example
    copy .env.example .env
    echo Please edit .env and set your GEMINI_API_KEY before running again
    exit /b 1
)

REM Ensure working directory is the script directory
cd /d %~dp0

REM Load environment variables from .env
for /f "tokens=*" %%i in ('type .env ^| findstr /v "^#"') do set %%i

REM Check required environment variables
if "%GEMINI_API_KEY%"=="" (
    echo Error: GEMINI_API_KEY not set in .env
    exit /b 1
)

echo.
echo Configuration:
echo   ILP Endpoint: %ILPENDPOINT%
echo   CW2 Endpoint: %CW2_ENDPOINT%
echo   Gemini Model: %GEMINI_MODEL%
echo.

REM Create virtual environment if needed
if not exist venv (
    echo Creating virtual environment...
    python -m venv venv
)

echo Activating virtual environment...
pushd %~dp0 >nul
if exist venv\Scripts\activate.bat (
    call venv\Scripts\activate.bat
) else (
    echo Creating virtual environment...
    python -m venv venv
    call venv\Scripts\activate.bat
)

echo Installing dependencies (if missing)...
venv\Scripts\python.exe -m pip install -q --upgrade pip
venv\Scripts\python.exe -m pip install -q -r requirements.txt

echo.
echo Starting services...
echo.

REM Start FastAPI in background using the venv python and write logs to uvicorn.log
echo Starting Dispatch Engine API on http://127.0.0.1:8002 (binding to localhost to avoid socket ACL issues)
start "Dispatch API" /B cmd /C "pushd %~dp0 && venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8002 --reload > uvicorn.log 2>&1"

REM Wait for API to become reachable on port 8002 (retry loop)
echo Waiting for API at http://127.0.0.1:8002 ...
set API_UP=0
set PORT=8002
set /A i=0
:wait_8002
if %i% GEQ 15 goto wait_8002_done
powershell -Command "try{(Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8002' -TimeoutSec 2) | Out-Null; exit 0}catch{exit 1}"
if %ERRORLEVEL% EQU 0 (
    set API_UP=1
    goto api_ready
)
set /A i=i+1
timeout /t 1 /nobreak > nul
goto wait_8002
:wait_8002_done

:api_ready
if "%API_UP%"=="1" (
    echo API is reachable on port 8002.
) else (
    echo WARNING: API did not become reachable on port 8002 after retries. Attempting fallback on port 8003
    REM Start fallback on 8003 and write to uvicorn_8003.log
    start "Dispatch API-8003" /B cmd /C "pushd %~dp0 && venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8003 --reload > uvicorn_8003.log 2>&1"

    echo Waiting for API at http://127.0.0.1:8003 ...
    set /A i=0
    set API_UP=0
    set PORT=8003
    :wait_8003
    if %i% GEQ 15 goto wait_8003_done
    powershell -Command "try{(Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8003' -TimeoutSec 2) | Out-Null; exit 0}catch{exit 1}"
    if %ERRORLEVEL% EQU 0 (
        set API_UP=1
        goto api_ready2
    )
    set /A i=i+1
    timeout /t 1 /nobreak > nul
    goto wait_8003
    :wait_8003_done
    :api_ready2
    if "%API_UP%"=="1" (
        echo API is reachable on port 8003.
    ) else (
        echo ERROR: API did not become reachable on ports 8002 or 8003. Showing last logs for diagnosis.
        if exist uvicorn_8003.log (
            echo ----- uvicorn_8003.log -----
            powershell -Command "Get-Content -Path uvicorn_8003.log -Tail 200 | Write-Output"
        )
        if exist uvicorn.log (
            echo ----- uvicorn.log -----
            powershell -Command "Get-Content -Path uvicorn.log -Tail 200 | Write-Output"
        )
    )
)

REM Set API URL for Streamlit (use %PORT% from whichever port worked)
if "%PORT%"=="8003" (
    set DISPATCH_API_URL=http://localhost:8003
) else (
    set DISPATCH_API_URL=http://localhost:8002
)

REM Start Streamlit in foreground (so the window stays open)
echo Starting Command Center UI on http://localhost:8501
echo.
echo =========================================
echo Services running:
echo   - API: %DISPATCH_API_URL% (logs: uvicorn.log)
echo   - UI:  http://localhost:8501
echo =========================================
echo.
venv\Scripts\python.exe -m streamlit run ui_app.py --server.port=8501 --server.address=0.0.0.0
popd >nul

@echo off
chcp 65001 >nul
cd /d "%~dp0"
set LOG=salida-build.txt

echo ===================================================
echo  RIKOS - levantando entorno de desarrollo
echo ===================================================
echo.

echo ===== RIKOS: salida de la ejecucion ===== > "%LOG%"
echo Fecha: %date% %time% >> "%LOG%"
echo. >> "%LOG%"

echo ----- versiones ----- >> "%LOG%"
java -version >> "%LOG%" 2>&1
echo. >> "%LOG%"
mvn -version >> "%LOG%" 2>&1
echo. >> "%LOG%"
docker --version >> "%LOG%" 2>&1
echo. >> "%LOG%"

echo [1/3] Levantando Postgres, RabbitMQ y Mailpit...
echo ----- docker compose up -d ----- >> "%LOG%"
docker compose up -d >> "%LOG%" 2>&1
echo Codigo de salida: %errorlevel% >> "%LOG%"
echo. >> "%LOG%"

echo [2/3] Estado de los contenedores...
echo ----- docker compose ps ----- >> "%LOG%"
docker compose ps >> "%LOG%" 2>&1
echo. >> "%LOG%"

echo [3/3] Compilando los cuatro modulos con Maven...
echo        (la primera vez descarga dependencias: puede tardar varios minutos)
echo ----- mvn clean install -DskipTests ----- >> "%LOG%"
call mvn -B clean install -DskipTests >> "%LOG%" 2>&1
echo Codigo de salida de Maven: %errorlevel% >> "%LOG%"
echo. >> "%LOG%"

echo ===== FIN ===== >> "%LOG%"

echo.
echo ===================================================
echo  Termino. El detalle quedo en:  %LOG%
echo ===================================================
echo.
pause

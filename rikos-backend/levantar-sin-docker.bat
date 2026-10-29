@echo off
chcp 65001 >nul
cd /d "%~dp0"

rem Levanta el backend SIN Docker, contra el PostgreSQL nativo de Windows (puerto 5432).
rem
rem Una sola vez, antes del primer uso: crear el usuario y las bases (ver docs/EMPEZAR.md)
rem   CREATE ROLE rikos LOGIN PASSWORD 'rikos';
rem   CREATE DATABASE rikos_operacion   OWNER rikos;
rem   CREATE DATABASE rikos_facturacion OWNER rikos;
rem
rem RabbitMQ y Mailpit no hacen falta: hoy el codigo no envia ni escucha mensajes,
rem solo tiene la dependencia. Se apaga su chequeo de salud para que /health no salga DOWN.
rem
rem Perfil "demo": al arrancar carga DATOS DE PRUEBA inventados (mesas, platos, stock,
rem pensionistas, mozos...). Son distintos de los datos del sistema que crean las
rem migraciones. Ver servicio-operacion/src/main/resources/demo/datos-de-prueba.sql.
rem Para arrancar sin ellos: set SPRING_PROFILES_ACTIVE= antes de llamar a este script.
rem
rem Uso:
rem   levantar-sin-docker.bat             operacion y gateway, cada uno en su ventana
rem   levantar-sin-docker.bat operacion   solo servicio-operacion, en esta ventana
rem   levantar-sin-docker.bat gateway     solo el gateway, en esta ventana

set "DB_PUERTO=5432"
set "MANAGEMENT_HEALTH_RABBIT_ENABLED=false"
set "DB_URL=jdbc:postgresql://localhost:%DB_PUERTO%/rikos_operacion"
if not defined SPRING_PROFILES_ACTIVE set "SPRING_PROFILES_ACTIVE=demo"

echo ===================================================
echo  RIKOS - backend sin Docker (Postgres en %DB_PUERTO%)
echo ===================================================

echo [1/2] Compilando contratos (lo usan los servicios)...
call "%~dp0mvnw.cmd" -q -B -pl contratos install -DskipTests
if errorlevel 1 (
    echo Fallo la compilacion de contratos.
    exit /b 1
)

if /i "%~1"=="operacion" (
    echo [2/2] servicio-operacion en http://localhost:8081/api
    cd /d "%~dp0servicio-operacion"
    call "%~dp0mvnw.cmd" spring-boot:run
    exit /b %errorlevel%
)

if /i "%~1"=="gateway" (
    echo [2/2] gateway en http://localhost:8080
    cd /d "%~dp0gateway"
    call "%~dp0mvnw.cmd" spring-boot:run
    exit /b %errorlevel%
)

echo [2/2] Abriendo servicio-operacion (8081) y gateway (8080) en sus ventanas...
start "RIKOS operacion" /d "%~dp0servicio-operacion" cmd /k ""%~dp0mvnw.cmd" spring-boot:run"
start "RIKOS gateway" /d "%~dp0gateway" cmd /k ""%~dp0mvnw.cmd" spring-boot:run"

echo.
echo  operacion: http://localhost:8081/api/salud
echo  gateway:   http://localhost:8080
echo  Para cerrar, cierra las dos ventanas.

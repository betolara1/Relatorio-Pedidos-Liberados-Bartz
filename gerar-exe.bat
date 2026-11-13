@echo off
echo ===================================================
echo   Gerando executavel do Pedidos Liberados
echo ===================================================

echo [1/4] Compilando e gerando JAR com dependencias...
call mvn clean package
if errorlevel 1 goto ERRO

echo [2/4] Limpando versao anterior...
if exist dist\PedidosLiberados rmdir /s /q dist\PedidosLiberados
if exist dist\PedidosLiberados.zip del /f /q dist\PedidosLiberados.zip

echo [3/4] Empacotando com jpackage...
jpackage --type app-image --name "PedidosLiberados" --input target --main-jar pedidos-liberados-1.0.0.jar --main-class br.com.bartz.pedidos.PedidosLiberadosApp --dest dist
if not exist "dist\PedidosLiberados\PedidosLiberados.exe" goto ERRO_JPACKAGE

if exist ".env" copy .env dist\PedidosLiberados\.env >nul 2>&1

echo [4/4] Compactando pasta para envio via WhatsApp...
powershell -NoProfile -Command "Compress-Archive -Path dist\PedidosLiberados -DestinationPath dist\PedidosLiberados.zip -Force"

echo ===================================================
echo SUCESSO!
echo.
echo 1. Pasta executavel: dist\PedidosLiberados\PedidosLiberados.exe
echo 2. Arquivo de envio: dist\PedidosLiberados.zip
echo.
echo Envie SEMPRE o arquivo PedidosLiberados.zip pelo WhatsApp!
echo ===================================================
goto FIM

:ERRO
echo [ERRO] Falha ao compilar com o Maven.
goto FIM

:ERRO_JPACKAGE
echo [ERRO] O executavel nao pode ser gerado pelo jpackage.
goto FIM

:FIM
pause

@echo off
title RemEdio Certo - Configuracao Inicial
echo ======================================================
echo           CONFIGURANDO PROJETO EM NOVO PC
echo ======================================================
echo.

:: Verifica se o Git está instalado
git --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERRO] Git nao encontrado. Instale o Git para continuar.
    pause
    exit /b
)

echo [+] Verificando atualizacoes do repositorio...
git pull origin main

echo.
echo [+] Limpando cache e preparando build...
if exist gradlew.bat (
    call gradlew.bat clean
    echo.
    echo [+] Gerando APK de teste para validar...
    call gradlew.bat assembleDebug
) else (
    echo [AVISO] 'gradlew.bat' nao encontrado. Verifique se esta na pasta raiz do projeto.
)

echo.
echo ======================================================
echo           CONFIGURACAO CONCLUIDA!
echo ======================================================
echo O projeto esta pronto para ser aberto no Android Studio.
pause

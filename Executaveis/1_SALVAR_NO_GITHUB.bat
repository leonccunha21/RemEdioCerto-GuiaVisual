@echo off
setlocal enabledelayedexpansion

title RemEdio Certo - Backup GitHub
echo ======================================================
echo           SALVANDO PROJETO NO GITHUB
echo ======================================================
echo.

:: Verifica se o Git está instalado
git --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERRO] Git nao encontrado. Instale o Git para continuar.
    pause
    exit /b
)

:: Pede mensagem de commit opcional
set /p commit_msg="Digite a mensagem do commit (vazio para 'Atualizacao Automatica'): "
if "!commit_msg!"=="" set commit_msg=Atualizacao Automatica de Modernizacao

echo.
echo [+] Adicionando arquivos...
git add .

echo [+] Criando commit...
git commit -m "!commit_msg!"

echo [+] Enviando para o servidor...
git push origin main

echo.
echo ======================================================
echo           CONCLUIDO COM SUCESSO!
echo ======================================================
pause

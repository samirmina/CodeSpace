@echo off
setlocal enabledelayedexpansion

SET JAVA_HOME=E:\.jdks\ms-21.0.10
SET PG_JDBC=C:\Users\Meena\.m2\repository\org\postgresql\postgresql\42.7.5\postgresql-42.7.5.jar
SET SRC=E:\CodeSpace\user-creator\src\main\java\com\chronos\usercrud
SET OUT=E:\CodeSpace\user-creator\out
SET PGSQL_DIR=E:\CodeSpace\pgsql
SET PG_BIN=%PGSQL_DIR%\bin
SET PG_DATA=%PGSQL_DIR%\data

echo Compiling CLI...
"%JAVA_HOME%\bin\javac.exe" -cp "%PG_JDBC%" -d "%OUT%" "%SRC%\User.java" "%SRC%\UserDAO.java" "%SRC%\UserCrudCLI.java"
if errorlevel 1 (
    echo Compilation failed
    pause
    exit /b 1
)
echo Compilation successful
echo.

echo Starting PostgreSQL...
if exist "%PG_DATA%\postmaster.pid" (
    del /f /q "%PG_DATA%\postmaster.pid"
)
"%PG_BIN%\pg_ctl.exe" -D "%PG_DATA%" -l "%PG_DATA%\logfile.log" start
if !errorlevel! neq 0 (
    echo Warning: pg_ctl start returned error, checking if server is already running...
)

echo Waiting for database to be ready...
set "RETRIES=0"
:wait_loop
set /a "RETRIES+=1"
if !RETRIES! gtr 60 (
    echo PostgreSQL did not become ready in time.
    pause
    exit /b 1
)
"%PG_BIN%\pg_isready.exe" -q -h localhost -p 5432
if !errorlevel! equ 0 (
    echo PostgreSQL is ready.
    echo Setting up database schema...
    REM Create chronos database if it doesn't exist
    "%PG_BIN%\psql.exe" -h localhost -p 5432 -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='chronos'" | findstr 1 >nul
    if !errorlevel! neq 0 (
        echo Creating chronos database...
        "%PG_BIN%\psql.exe" -h localhost -p 5432 -U postgres -a -f "%PGSQL_DIR%\create_db.sql"
        if !errorlevel! neq 0 (
            echo FAILED to create chronos database.
            pause
            "%PG_BIN%\pg_ctl.exe" stop -D "%PG_DATA%" -m fast
            exit /b 1
        )
    )
    REM Load schema into chronos database if users table doesn't exist
    "%PG_BIN%\psql.exe" -h localhost -p 5432 -U postgres -d chronos -tAc "SELECT 1 FROM information_schema.tables WHERE table_name='users'" | findstr 1 >nul
    if !errorlevel! neq 0 (
        echo Loading schema...
        "%PG_BIN%\psql.exe" -h localhost -p 5432 -U postgres -d chronos -a -f "%PGSQL_DIR%\create_users_table.sql"
        if !errorlevel! neq 0 (
            echo FAILED to load schema.
            pause
            "%PG_BIN%\pg_ctl.exe" stop -D "%PG_DATA%" -m fast
            exit /b 1
        )
    )
    goto :run_cli
)
timeout /t 1 /nobreak >nul
goto :wait_loop

:run_cli
echo Running User Manager CLI...
"%JAVA_HOME%\bin\java.exe" -cp "%OUT%;%PG_JDBC%" com.chronos.usercrud.UserCrudCLI
set "APP_EXIT_CODE=%errorlevel%"

echo.
echo Shutting down PostgreSQL...
"%PG_BIN%\pg_ctl.exe" stop -D "%PG_DATA%" -m fast -w
if !errorlevel! equ 0 (
    echo PostgreSQL shut down successfully.
) else (
    echo Warning: PostgreSQL may still be running.
)

endlocal & exit /b %APP_EXIT_CODE%

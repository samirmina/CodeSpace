@echo off
SET JAVA_HOME=E:\.jdks\ms-21.0.10
SET JAVAFX_HOME=javafx\javafx-sdk-21.0.2\lib
SET PG_JDBC=C:\Users\Meena\.m2\repository\org\postgresql\postgresql\42.7.5\postgresql-42.7.5.jar
SET SRC=E:\CodeSpace\user-creator\src\main\java\com\chronos\usercrud
SET OUT=E:\CodeSpace\user-creator\out

echo Compiling...
"%JAVA_HOME%\bin\javac.exe" --module-path "%JAVAFX_HOME%" --add-modules javafx.controls,javafx.graphics,javafx.base -cp "%PG_JDBC%" -d "%OUT%" "%SRC%\User.java" "%SRC%\UserDAO.java" "%SRC%\UserCrudApp.java"
if errorlevel 1 (
    echo Compilation failed
    pause
    exit /b 1
)
echo Compilation successful
pause

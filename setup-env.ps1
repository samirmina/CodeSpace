# setup-env.ps1 - Put in your project root (E:\CodeSpace\)
# Sets up Java 21 and Maven environment variables for the Chronos project.

$projectRoot = Split-Path $PSScriptRoot

# JDK 21
$jdkPath = "E:\.jdks\ms-21.0.10"
if (Test-Path "$jdkPath\bin\java.exe") {
    $env:JAVA_HOME = $jdkPath
    $env:Path = "$jdkPath\bin;" + $env:Path
}

# Maven
$mavenPath = "E:\CodeSpace\apache-maven-3.9.16"
if (Test-Path "$mavenPath\bin\mvn.cmd") {
    $env:M2_HOME = $mavenPath
    $env:MAVEN_HOME = $mavenPath
    $env:Path = "$mavenPath\bin;" + $env:Path
}

# Verify
Write-Host "Java:"
java --version
Write-Host "Maven:"
mvn --version

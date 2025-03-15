@echo off

rem Check if JAVA_HOME is set
if "%JAVA_HOME%" == "" (
    echo JAVA_HOME environment variable is not set. Please set it to the Java installation directory.
    exit /b 1
)

set /p agentJar="Enter path to the agent JAR file: "
set /p configFile="Enter path to the agent config file: "
set /p agentArgs="Enter arguments to pass to the agent: "
set /p pid="Enter PID of the target JVM: "

if "%pid%" == "" (
    set pid=0
)

"%JAVA_HOME%\bin\java" -cp .;"%agentJar%;%JAVA_HOME%\lib\tools.jar" com.asm.mja.attach.AgentAttachCLI -agentJar "%agentJar%" -configFile "%configFile%" -args "%agentArgs%" -pid %pid%
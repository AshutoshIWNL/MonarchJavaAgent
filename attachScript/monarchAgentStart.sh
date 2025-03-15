#!/bin/bash

# Check if JAVA_HOME is set
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME environment variable is not set. Please set it to the Java installation directory."
    exit 1
fi

read -p "Enter path to the agent JAR file: " agentJar
read -p "Enter path to the agent config file: " configFile
read -p "Enter arguments to pass to the agent: " agentArgs
read -p "Enter PID of the target JVM: " pid

if [ -z "$pid" ]; then
    pid=0
fi

"$JAVA_HOME/bin/java" -cp .:"$agentJar":"$JAVA_HOME/lib/tools.jar" com.asm.mja.attach.AgentAttachCLI -agentJar "$agentJar" -configFile "$configFile" -args "$agentArgs" -pid $pid
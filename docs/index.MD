---
title: Monarch Java Agent
layout: default
---

<link rel="stylesheet" type="text/css" href="style.css">

Monarch is a powerful Java agent for method monitoring and analysis. It offers various features to track method execution time, print stack traces, log method arguments and return values, capture heap dumps, and gather JVM-related information.

"Arise, shadows of execution!"

## Key Abilities

* **Shadow Time Capture:** Command the agent to reveal the precise time taken for any method's execution.
* **Shadow Trace Revelation:** Unveil the stack trace upon a method's invocation, illuminating the path of execution.
* **Shadow Argument Whispers:** Capture and log the arguments passed to a method, discerning the secrets within.
* **Shadow Return Echoes:** Record the return values of methods, listening to the echoes of their completion.
* **Shadow Code Infusion:** Inject your own commands into the flow of execution, altering the very fabric of your application.
* **Shadow Memory Grasp:** Seize a heap dump at critical moments, capturing the memory landscape.
* **Shadow System Gaze:** Peer into the system flags and JVM options, understanding the environment.
* **Shadow Resource Monitoring:** Track heap and CPU usage, ensuring the shadows remain under control.
* **Shadow Alert Sentinels:** Deploy alert emails to notify you of critical resource usage, maintaining vigilance.

## MonArgs

| Argument        | Description |
|----------------|-------------|
| `configFile`   | Path to the configuration file specifying agent behavior. |
| `agentLogFileDir` | Directory where initialization logs will be written. |
| `agentLogLevel` | Log verbosity level (`DEBUG`, `INFO`, `WARN`, `ERROR`). |
| `smtpProperties` | Path to SMTP configuration for sending alert emails. |


## Summoning the Monarch

You can attach Monarch-Java-Agent either during startup or during runtime.

**For startup:**

1. Download the latest agent JAR file from releases page.
2. Start your Java application using the `-javaagent` option.
3. Specify the configuration file and other options as needed.

Example command to attach the agent:

```bash 
java -Xbootclasspath/a:/path/to/monarch-java-agent.jar -javaagent:/path/to/monarch-java-agent.jar=configFile=/path/to/config.yaml,agentLogFileDir=/path/to/log/dir,agentLogLevel=DEBUG,smtpProperties=/path/to/smtpProperties.props YourMainClass
```

**For Runtime:**

1. Download the latest agent JAR file from releases page.
2. Start your application.
3. Run "monarchAgentStart.bat"/"monarchAgentStart.sh" and provide the requested details.

Example command run:
```bash 
C:\Users\ashut\monarch-java-agent\attachScript> .\monarchAgentStart.bat
Enter path to the agent JAR file: C:\Users\ashut\monarch-java-agent\target\monarch-java-agent-1.0-SNAPSHOT.jar
Enter path to the agent config file: C:\Users\ashut\monarch-java-agent\sampleConfig\mConfig.yaml
Enter arguments to pass to the agent: agentLogFileDir=C:\Users\ashut\manualTesting,agentLogLevel=DEBUG,smtpProperties=/path/to/smtpProperties.props
Enter PID of the target JVM (press Enter to use current JVM): 24300
Agent attached successfully to PID 24300
```

## Commanding the Shadows

Below is a sample configuration YAML that you can use with Monarch-Java-Agent:

```yaml
shouldInstrument: true
configRefreshInterval: 15
traceFileLocation: C:\\TraceFileDumps
agentRules:
  - ClassA::methodA@INGRESS::STACK
  - ClassA::methodA@INGRESS::ARGS
  - ClassA::methodA@EGRESS::RET
  - ClassA::methodB@INGRESS::ARGS
  - ClassA::methodB@INGRESS::STACK
  - ClassA::methodB@EGRESS::STACK
  - ClassA::methodB@EGRESS::RET
  - ClassB::methodC@PROFILE
  - ClassB::methodC@INGRESS::HEAP
  - ClassB::methodC@INGRESS::ADD::[System.out.println(20);]
  - ClassA::methodA@INGRESS::ADD::[System.out.println(this.getClass().getName());]
  - ClassA::methodA@CODEPOINT(11)::ADD::[System.out.println(499);]
  - ClassA::methodA@CODEPOINT(11)::ADD::[System.out.println(499 + "," + "Ashutosh Mishra");]
printClassLoaderTrace: true
printJVMSystemProperties: true
printEnvironmentVariables: true
printJVMHeapUsage: true
printJVMCpuUsage: true
maxHeapDumps: 3
sendAlertEmails: true
emailRecipientList:
  - abc@example.com
  - ashutosh@asm.com
```

## The Monarch's Decrees

The rule syntax for Monarch-Java-Agent follows the format:

```plaintext
<FQCN>::<MethodName>@<EVENT>::<ACTION>
```

Where:

- `<FQCN>`: Fully Qualified Class Name.
- `<MethodName>`: Name of the method.
- `<EVENT>`: Event at which the action should be performed. Possible values are:
    - INGRESS
    - EGRESS
    - CODEPOINT
    - PROFILE (Note: PROFILE is a special case and no ACTION is required along with it.)
- `<ACTION>`: Action to be performed. Possible values are:
    - STACK: Print stack trace.
    - HEAP: Capture heap dump.
    - ARGS: Log method arguments.
    - RET: Log method return value.
    - ADD: Add custom code.


## Forging the Shadow

```bash
git https://github.com/AshutoshIWNL/MonarchJavaAgent.git
cd MonarchJavaAgent
mvn clean package
```

## The Architect of Shadows

- **Ashutosh Mishra** (https://github.com/AshutoshIWNL)

## The Shadow's Pact

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
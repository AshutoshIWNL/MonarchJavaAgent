# MonarchJavaAgent

MonarchJavaAgent is a powerful Java agent for method monitoring and analysis. It offers various features to track method execution time, print stack traces, log method arguments and return values, capture heap dumps, and gather JVM-related information.

## Features

- **Stack Trace Printing**: Print the stack trace when a certain method is invoked. Supports conditional printing only if a specific class/method appears in the current stack trace.
- **Return Value Logging**: Log the return value of a method.
- **Custom code addition**: Add custom code to a method.
- **Heap Dump Capture**: Take a heap dump when a method is invoked or exits.
- **Method Execution Time**: Print the time taken for a method's execution (Bit buggy currently and needs bytecode verification to be turned off).
- **System Flags Printing**: Print system flags of the target application.
- **JVM Options Printing**: Print JVM options of the target application.
- **JVM Heap Usage Details**: Print JVM heap usage details of the target application.
- **JVM CPU Usage Details**: Print JVM CPU usage details of the target application.
- **Alert Emails**: Sends alert emails for high heap/ high CPU usage in the target application. 

## Agent Arguments

| Argument          | Description                                                               |
|-------------------|---------------------------------------------------------------------------|
| `configFile`      | Path to the configuration file specifying agent behavior **[Mandatory]**. |
| `agentLogFileDir` | Directory where initialization logs will be written.                      |
| `agentLogLevel`   | Log verbosity level (`DEBUG`, `INFO`, `WARN`, `ERROR`).                   |
| `smtpProperties`  | Path to SMTP configuration for sending alert emails.                      |
| `agentJarPath`    | Path to the MonarchJavaAgent jar **[Mandatory for startup attach]**.      |


## Usage

You can attach Monarch-Java-Agent either during startup or during runtime.

**For startup:**

1. Download the latest agent JAR file from releases page.
2. Start your Java application using the `-javaagent` option.
3. Specify the configuration file and other options as needed.

Example command to attach the agent:

```bash 
java -Xverify:none -Xbootclasspath/a:/path/to/MonarchJavaAgent.jar -javaagent:/path/to/MonarchJavaAgent.jar=configFile=/path/to/config.yaml,agentLogFileDir=/path/to/log/dir,agentLogLevel=DEBUG,smtpProperties=/path/to/smtpProperties.props,agentJarPath=/path/to/MonarchJavaAgent.jar YourMainClass
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

## Configuration

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
  - ClassA::methodB@INGRESS::STACK::[com.asm]
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

## Rule Syntax

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


## Building from Source

```bash
git https://github.com/AshutoshIWNL/MonarchJavaAgent.git
cd MonarchJavaAgent
mvn clean package
```

## Author

- **Ashutosh Mishra** (https://github.com/AshutoshIWNL)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
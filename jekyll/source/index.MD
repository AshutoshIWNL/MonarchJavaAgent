---
title: Monarch Java Agent
theme: null
---
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/AshutoshIWNL/MonarchJavaAgent)
<link rel="icon" type="image/x-icon" href="favicon.ico">
<link rel="stylesheet" type="text/css" href="style.css">

# Monarch Java Agent

Monarch is a hybrid Java agent for production diagnostics and JVM observation. It lets you attach targeted bytecode instrumentation to a live JVM, expose Prometheus-compatible JVM metrics, and move between instrumenter, observer, and hybrid workflows without changing the application code.

"Arise, shadows of execution!"

## Why Monarch

Use Monarch when you need to:

* inspect a hot method in production without redeploying
* inject a temporary runtime debug probe to validate a hypothesis
* expose JVM health signals to Prometheus and Grafana
* combine traces and metrics during incident response

## Key Abilities

* **Shadow Trace Revelation:** Unveil the stack trace upon a method's invocation.
* **Shadow Argument Whispers:** Capture and log the arguments passed to a method.
* **Shadow Return Echoes:** Record the return values of methods.
* **Shadow Code Infusion:** Inject your own diagnostic commands into the flow of execution.
* **Shadow Time Capture:** Measure method execution time.
* **Shadow Memory Grasp:** Seize a heap dump at critical moments.
* **Shadow Resource Monitoring:** Track heap, CPU, GC, thread, and classloader behavior.
* **Shadow Metrics Divination:** Expose Prometheus-compatible JVM metrics at `/metrics`.
* **Shadow Alert Sentinels:** Deploy alert emails for critical resource usage.

## MonArgs

| Argument | Description |
|---|---|
| `configFile` | Path to the configuration file specifying agent behavior **[Mandatory]**. |
| `agentLogFileDir` | Directory where initialization logs will be written. |
| `agentLogLevel` | Log verbosity level (`DEBUG`, `INFO`, `WARN`, `ERROR`). |
| `smtpProperties` | Path to SMTP configuration for sending alert emails. |
| `agentJarPath` | Path to the MonarchJavaAgent jar **[Mandatory for startup attach]**. |

## Summoning the Monarch

You can attach MonarchJavaAgent either during startup or during runtime.

**For startup:**

1. Download the latest agent JAR file from releases.
2. Start your Java application using `-javaagent`.
3. Pass the config file and bootstrap arguments.

```bash
java -Xverify:none -javaagent:/path/to/MonarchJavaAgent.jar=configFile=/path/to/config.yaml,agentLogFileDir=/path/to/log/dir,agentLogLevel=DEBUG,smtpProperties=/path/to/smtpProperties.props,agentJarPath=/path/to/MonarchJavaAgent.jar YourMainClass
```

**For runtime attach:**

1. Start your application normally.
2. Run `monarchAgentStart.bat` or `monarchAgentStart.sh`.
3. Provide the agent jar, config file, bootstrap arguments, and target PID.

## Commanding the Shadows

Monarch supports a nested, mode-aware YAML structure.

* `instrumenter`: bytecode instrumentation only
* `observer`: JVM observation and metrics only
* `hybrid`: both instrumentation and observer features

Canonical sample configuration:

```yaml
mode: hybrid

instrumentation:
  enabled: true
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

observer:
  enabled: true
  printClassLoaderTrace: true
  printJVMSystemProperties: true
  printEnvironmentVariables: true
  metrics:
    exposeHttp: true
    port: 9090
    heapUsage: true
    cpuUsage: true
    threadUsage: true
    gcStats: true
    classLoaderStats: true

alerts:
  enabled: true
  maxHeapDumps: 3
  emailRecipientList:
    - abc@example.com
    - ashutosh@asm.com
```

Legacy flat keys are still accepted, but nested config is the default and takes precedence when both forms are present.

## The Monarch's Decrees

Rule syntax:

```plaintext
<FQCN>::<MethodName>@<EVENT>::<ACTION>
```

Where:

* `<FQCN>` is the fully qualified class name
* `<MethodName>` is the method name
* `<EVENT>` is one of `INGRESS`, `EGRESS`, `CODEPOINT`, `PROFILE`
* `<ACTION>` is one of `STACK`, `HEAP`, `ARGS`, `RET`, `ADD`

## Shadow Metrics Gate

If `observer.metrics.exposeHttp: true` is enabled, Monarch starts a lightweight HTTP server exposing JVM metrics.

Default endpoints:

* `http://localhost:9090/metrics`
* `http://localhost:9090/metrics.json`

Prometheus scrape config:

```yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: "monarch-java-agent"
    static_configs:
      - targets: ["127.0.0.1:9090"]
```

Common Prometheus queries:

* `monarch_jvm_heap_used_bytes`
* `monarch_jvm_cpu_process_percent`
* `monarch_jvm_threads_current`
* `monarch_jvm_classloader_loaded`

Example Grafana dashboard:

![Grafana dashboard for MonarchJavaAgent metrics](grafana-dashboard.png.jpg)

## Forging the Shadow

```bash
git clone https://github.com/AshutoshIWNL/MonarchJavaAgent.git
cd MonarchJavaAgent
mvn clean package
```

## The Architect of Shadows

* **Ashutosh Mishra** (https://github.com/AshutoshIWNL)

## The Shadow's Pact

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

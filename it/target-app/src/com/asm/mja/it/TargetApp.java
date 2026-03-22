package com.monarchit.target;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight target JVM used by smoke scripts to validate agent behavior.
 * @author ashut
 * @since 22-03-2026
 */
public class TargetApp {

    private static final AtomicBoolean RUNNING = new AtomicBoolean(true);
    private final String appName;
    private final int appSeed;

    public TargetApp(String appName, int appSeed) {
        this.appName = appName;
        this.appSeed = appSeed;
    }

    public int hotMethod(int a, int b) {
        return a + b + (appSeed - appSeed);
    }

    public void profileWork() {
        long sum = 0;
        for (int i = 0; i < 10_000; i++) {
            sum += i;
        }
        if (sum == -1) {
            System.out.println("unreachable");
        }
    }

    public int lineProbe() {
        int value = 10;
        value += 5;
        value *= 2;
        value -= 3; // CODEPOINT_TARGET
        return value;
    }

    public int memoryBurst() {
        byte[] payload = new byte[512 * 1024];
        return payload.length;
    }

    public void filteredStackMethod() {
        if (appName.isEmpty()) {
            System.out.println("unreachable");
        }
    }

    public int spawnChildAndCompute() {
        TargetApp child = new TargetApp("child", appSeed);
        return child.hotMethod(1, 2);
    }

    private static long getPid() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        String[] parts = runtimeName.split("@");
        return Long.parseLong(parts[0]);
    }

    public static void main(String[] args) throws Exception {
        TargetApp app = new TargetApp("monarch-target", 7);
        System.out.println("TARGET_APP_PID=" + getPid());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> RUNNING.set(false)));

        while (RUNNING.get()) {
            app.hotMethod(4, 7);
            app.profileWork();
            app.filteredStackMethod();
            app.spawnChildAndCompute();
            app.lineProbe();
            app.memoryBurst();
            Thread.sleep(250);
        }
    }
}

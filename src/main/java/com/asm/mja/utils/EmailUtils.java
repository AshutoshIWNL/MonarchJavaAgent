package com.asm.mja.utils;

import com.asm.mja.logging.TraceFileLogger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author ashut
 * @since 09-02-2025
 */

public class EmailUtils {
    private static final TraceFileLogger logger = TraceFileLogger.getInstance();
    private static String SMTP_HOST;
    private static String SMTP_PORT;
    private static boolean SMTP_AUTH;
    private static String SENDER_EMAIL;
    private static String SENDER_PASSWORD;
    private static boolean SMTP_TLS;
    private static boolean isSendAlertEmail;
    private static List<String> emailRecipientList;
    private static volatile Session emailSession = null;
    private static boolean isConfigured = false;

    public static void init(String smtpPropertiesFile, boolean sendAlertEmail, List<String> recipientList) {
        if(smtpPropertiesFile != null) {
            try (FileInputStream input = new FileInputStream(smtpPropertiesFile)) {
                Properties smtpProps = new Properties();
                smtpProps.load(input);
                SMTP_HOST = smtpProps.getProperty("smtp.host");
                SMTP_PORT = smtpProps.getProperty("smtp.port");
                SMTP_AUTH = Boolean.parseBoolean(smtpProps.getProperty("smtp.auth"));
                SENDER_EMAIL = smtpProps.getProperty("smtp.user");
                SENDER_PASSWORD = smtpProps.getProperty("smtp.password");
                SMTP_TLS = Boolean.parseBoolean(smtpProps.getProperty("smtp.tls"));
                isConfigured = true;
            } catch (IOException e) {
                logger.error("Error loading SMTP properties file: " + smtpPropertiesFile + "; Emails will not be sent", e);
            }
        }
        isSendAlertEmail = sendAlertEmail;
        emailRecipientList = recipientList;
    }

    private static Properties createMailProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        properties.put("mail.smtp.auth", SMTP_AUTH);
        properties.put("mail.smtp.starttls.enable", SMTP_TLS);
        return properties;
    }

    public static void sendDeadlockAlert(int deadlockedThreadCount) {
        if(!isSendAlertEmail || !isConfigured)
            return;

        String subject = "CRITICAL: JVM Deadlock Detected";
        String body = String.format(
                "CRITICAL: Deadlock detected in JVM!\n\n" +
                        "Number of deadlocked threads: %d\n" +
                        "Timestamp: %s\n\n" +
                        "This requires immediate attention as deadlocked threads are blocking application execution.\n" +
                        "Please investigate the thread dump and resolve the deadlock.",
                deadlockedThreadCount,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        sendEmail(subject, body);
    }

    public static void sendThreadCountAlert(int threadCount) {
        if(!isSendAlertEmail || !isConfigured)
            return;

        String subject = "WARNING: High JVM Thread Count Alert";
        String body = String.format(
                "Warning: JVM thread count has exceeded the threshold.\n\n" +
                        "Current thread count: %d\n" +
                        "Threshold: %d\n" +
                        "Timestamp: %s\n\n" +
                        "High thread count may indicate:\n" +
                        "- Thread leaks\n" +
                        "- Excessive concurrent operations\n" +
                        "- Resource pool exhaustion\n\n" +
                        "Please investigate thread dumps and monitor for increasing trend.",
                threadCount,
                5000,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        sendEmail(subject, body);
    }

    public static void sendHeapUsageAlert(String heapUsage) {
        if(!isSendAlertEmail || !isConfigured)
            return;

        String subject = "WARNING: High JVM Heap Usage Alert";
        String body = String.format(
                "Warning: JVM heap memory usage has exceeded the threshold.\n\n" +
                        "Details:\n" +
                        "- Current heap usage: %s%%\n" +
                        "- Threshold: 90%%\n" +
                        "- Timestamp: %s\n\n" +
                        "High heap usage may indicate:\n" +
                        "- Memory leaks\n" +
                        "- Insufficient heap size allocation\n" +
                        "- Excessive object creation\n" +
                        "- Inefficient garbage collection\n\n" +
                        "Recommended actions:\n" +
                        "1. Monitor GC activity and frequency\n" +
                        "2. Analyze heap dumps for memory leaks\n" +
                        "3. Review recent deployments for memory-intensive changes\n" +
                        "4. Consider increasing heap size if consistently high\n" +
                        "5. If usage continues to rise, prepare for potential OutOfMemoryError",
                heapUsage,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        sendEmail(subject, body);
    }

    public static void sendCPULoadAlert(String cpuUsage) {
        if(!isSendAlertEmail || !isConfigured) {
            return;
        }

        String subject = "WARNING: High JVM CPU Usage Alert";
        String body = String.format(
                "Warning: JVM CPU usage has exceeded the threshold.\n\n" +
                        "Details:\n" +
                        "- Current CPU load: %s%%\n" +
                        "- Threshold: 90%%\n" +
                        "- Timestamp: %s\n\n" +
                        "High CPU usage may indicate:\n" +
                        "- Inefficient algorithms or loops\n" +
                        "- Excessive garbage collection activity\n" +
                        "- High request volume\n" +
                        "- Resource contention or blocking operations\n\n" +
                        "Recommended actions:\n" +
                        "1. Check thread dumps for CPU-intensive threads\n" +
                        "2. Review GC logs - excessive GC can cause high CPU\n" +
                        "3. Profile the application to identify hot spots\n" +
                        "4. Monitor request rates and thread pool usage\n" +
                        "5. Consider scaling if load is legitimate traffic",
                cpuUsage,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        sendEmail(subject, body);
    }

    public static void sendGCAlert(double gcTimePercent) {
        if(!isSendAlertEmail || !isConfigured)
            return;

        String subject = "WARNING: High JVM Garbage Collection Activity";
        String body = String.format(
                "Warning: JVM is spending excessive time in garbage collection.\n\n" +
                        "Details:\n" +
                        "- Time spent in GC: %.2f%%\n" +
                        "- Threshold: 10%%\n" +
                        "- Timestamp: %s\n\n" +
                        "High GC activity may indicate:\n" +
                        "- Memory pressure (heap size too small)\n" +
                        "- Memory leaks causing frequent collections\n" +
                        "- High allocation rate (creating too many objects)\n" +
                        "- Inefficient object lifecycle management\n\n" +
                        "Recommended actions:\n" +
                        "1. Review heap usage - may need to increase heap size\n" +
                        "2. Analyze GC logs for collection patterns\n" +
                        "3. Check for memory leaks with heap dumps\n" +
                        "4. Profile object allocation hotspots\n" +
                        "5. Consider tuning GC algorithm (G1GC, ZGC, etc.)\n" +
                        "6. If >50%% time in GC, application performance is severely impacted",
                gcTimePercent,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        sendEmail(subject, body);
    }

    public static void sendMetaspaceAlert(String metaspaceUsage, double threshold) {
        if(!isSendAlertEmail || !isConfigured)
            return;

        String subject = String.format("WARNING: High Metaspace Usage - %s%%", metaspaceUsage);
        String body = String.format(
                "Warning: JVM Metaspace usage has exceeded the threshold.\n\n" +
                        "Details:\n" +
                        "- Current Metaspace usage: %s%%\n" +
                        "- Threshold: %.0f%%\n" +
                        "- Timestamp: %s\n\n" +
                        "High Metaspace usage may indicate:\n" +
                        "- Classloader leaks (classes/classloaders not being garbage collected)\n" +
                        "- Excessive dynamic class generation (proxies, reflection)\n" +
                        "- Too many classes loaded\n" +
                        "- Insufficient Metaspace size allocation\n\n" +
                        "Recommended actions:\n" +
                        "1. Check for classloader leaks (common in app servers with redeployments)\n" +
                        "2. Review dynamic proxy usage and code generation libraries\n" +
                        "3. Analyze loaded classes with jcmd or visualvm\n" +
                        "4. Consider increasing -XX:MaxMetaspaceSize if legitimate usage\n" +
                        "5. If continuously growing, indicates a classloader leak\n" +
                        "6. Review hot deployment/redeployment strategies",
                metaspaceUsage,
                threshold * 100,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        sendEmail(subject, body);
    }

    public static void sendClassLoadRateAlert(double classLoadRate, int threshold) {
        if(!isSendAlertEmail || !isConfigured)
            return;

        String subject = String.format("WARNING: High Class Loading Rate - %.0f classes/min", classLoadRate);
        String body = String.format(
                "Warning: JVM is loading classes at an unusually high rate.\n\n" +
                        "Details:\n" +
                        "- Current load rate: %.1f classes/minute\n" +
                        "- Threshold: %d classes/minute\n" +
                        "- Timestamp: %s\n\n" +
                        "High class loading rate may indicate:\n" +
                        "- Classloader leak (creating new classloaders repeatedly)\n" +
                        "- Excessive dynamic class generation\n" +
                        "- Plugin or module loading issues\n" +
                        "- Hot deployment problems\n\n" +
                        "Recommended actions:\n" +
                        "1. Check for classloader leaks in application server\n" +
                        "2. Review dynamic proxy and bytecode generation usage\n" +
                        "3. Monitor Metaspace growth alongside class loading\n" +
                        "4. Investigate reflection-heavy code paths\n" +
                        "5. Review hot deployment/redeployment configurations",
                classLoadRate,
                threshold,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        sendEmail(subject, body);
    }

    private static void sendEmail(String subject, String body) {
        Properties properties = createMailProperties();

        try {
            MimeMessage message = new MimeMessage(getSession(properties));
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            if (emailRecipientList != null && !emailRecipientList.isEmpty()) {
                for (String recipient : emailRecipientList) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                }
            } else {
                logger.warn("Email recipient list is null or empty. No alert email will be sent.");
            }
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            logger.trace("Email sent successfully");
        } catch (MessagingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static Session getSession(Properties properties) {
        if(emailSession == null) {
            synchronized (EmailUtils.class) {
                if (emailSession == null) {
                    emailSession = createSession(properties); // Create session once
                }
            }
        }
        return emailSession;
    }

    private static Session createSession(Properties properties) {
        Session session = null;
        if(SMTP_AUTH) {
            session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });
        } else {
            session = Session.getInstance(properties);
        }
        return session;
    }
}

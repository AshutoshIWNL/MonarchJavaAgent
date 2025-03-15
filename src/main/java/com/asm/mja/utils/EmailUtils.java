package com.asm.mja.utils;

import com.asm.mja.logging.TraceFileLogger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
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

    public static void init(String smtpPropertiesFile, boolean sendAlertEmail, List<String> recipientList) {
        try (FileInputStream input = new FileInputStream(smtpPropertiesFile)) {
            Properties smtpProps = new Properties();
            smtpProps.load(input);
            SMTP_HOST = smtpProps.getProperty("smtp.host");
            SMTP_PORT = smtpProps.getProperty("smtp.port");
            SMTP_AUTH = Boolean.parseBoolean(smtpProps.getProperty("smtp.auth"));
            SENDER_EMAIL = smtpProps.getProperty("smtp.user");
            SENDER_PASSWORD = smtpProps.getProperty("smtp.password");
            SMTP_TLS = Boolean.parseBoolean(smtpProps.getProperty("smtp.tls"));
        } catch (IOException e) {
            logger.error("Error loading SMTP properties file: " + smtpPropertiesFile, e);
            throw new RuntimeException("Failed to load SMTP properties", e);
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

    public static void sendHeapUsageAlert(String heapUsage) {
        if(!isSendAlertEmail)
            return;
        String subject = "JVM Heap Usage Alert";
        String body = "Warning: Your JVM heap usage has exceeded 90%. Current usage: " + heapUsage + "M";

        sendEmail(subject, body);
    }

    public static void sendCPULoadAlert(String cpuUsage) {
        if(!isSendAlertEmail) {
            return;
        }
        String subject = "JVM CPU Load Alert";
        String body = "Warning: Your JVM CPU usage has exceeded 90%. Current CPU load: " + cpuUsage + "%";
        EmailUtils.sendEmail(subject, body); // This can be refactored if needed
    }

    private static void sendEmail(String subject, String body) {
        Properties properties = createMailProperties();

        try {
            MimeMessage message = new MimeMessage(getSession(properties));
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            for (String recipient : emailRecipientList) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
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

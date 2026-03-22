package com.asm.mja.bootstrap;

import com.asm.mja.logging.AgentLogger;
import com.asm.mja.utils.EmailUtils;

import java.util.List;

/**
 * Bootstraps SMTP configuration for alert emails.
 * @author ashut
 * @since 22-03-2026
 */
public class SmtpBootstrap {

    private SmtpBootstrap() {
    }

    public static void setupSMTP(String agentArgs, boolean isSendAlertEmail, List<String> emailRecipientList) {
        if (isSendAlertEmail) {
            String smtpFile = fetchSMTPProperties(agentArgs);
            if (smtpFile == null || smtpFile.isEmpty()) {
                AgentLogger.warning("Alert email is enabled, but SMTP config file is missing in agent args. Email alerts will be disabled.");
            }
            EmailUtils.init(smtpFile, true, emailRecipientList);
        }
    }

    private static String fetchSMTPProperties(String agentArgs) {
        String smtpPropertiesFile = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("smtpProperties")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        smtpPropertiesFile = prop[1];
                    }
                }
            }
        }
        return smtpPropertiesFile;
    }
}

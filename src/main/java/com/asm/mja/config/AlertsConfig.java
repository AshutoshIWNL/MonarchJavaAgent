package com.asm.mja.config;

import java.util.List;

/**
 * Nested alerts configuration section.
 * @author ashut
 * @since 22-03-2026
 */
public class AlertsConfig {
    private Boolean enabled;
    private Integer maxHeapDumps;
    private List<String> emailRecipientList;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getMaxHeapDumps() {
        return maxHeapDumps;
    }

    public void setMaxHeapDumps(Integer maxHeapDumps) {
        this.maxHeapDumps = maxHeapDumps;
    }

    public List<String> getEmailRecipientList() {
        return emailRecipientList;
    }

    public void setEmailRecipientList(List<String> emailRecipientList) {
        this.emailRecipientList = emailRecipientList;
    }
}

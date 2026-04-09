package com.bookmyjuice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "chargebee.sync")
public class ChargebeeSyncConfig {
    
    /**
     * Whether to run sync on application startup
     */
    private boolean enableStartupSync = true;
    
    /**
     * Batch size for processing items/customers
     */
    private int batchSize = 100;
    
    /**
     * Number of parallel threads for sync operations
     */
    private int threadPoolSize = 3;
    
    /**
     * Timeout for sync operations in minutes
     */
    private int syncTimeoutMinutes = 30;

    // Getters and setters
    public boolean isEnableStartupSync() {
        return enableStartupSync;
    }

    public void setEnableStartupSync(boolean enableStartupSync) {
        this.enableStartupSync = enableStartupSync;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public int getSyncTimeoutMinutes() {
        return syncTimeoutMinutes;
    }

    public void setSyncTimeoutMinutes(int syncTimeoutMinutes) {
        this.syncTimeoutMinutes = syncTimeoutMinutes;
    }
}
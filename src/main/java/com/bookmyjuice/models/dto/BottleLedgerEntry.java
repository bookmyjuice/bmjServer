package com.bookmyjuice.models.dto;

import java.util.Date;

/**
 * Represents a single entry in the BottleLedger — a computed view of
 * bottle transactions aggregated by customer and bottle type.
 * 
 * This is a DTO, not an entity. It is assembled by BottleTrackingService
 * from the bottle_transactions table.
 */
public class BottleLedgerEntry {

    private String customerId;
    private String bottleType;
    private int totalIssued;
    private int totalReturned;
    private int totalBroken;
    private int outstanding;
    private Date lastTransactionAt;

    public BottleLedgerEntry() {}

    public BottleLedgerEntry(String customerId, String bottleType,
                             int totalIssued, int totalReturned,
                             int totalBroken, Date lastTransactionAt) {
        this.customerId = customerId;
        this.bottleType = bottleType;
        this.totalIssued = totalIssued;
        this.totalReturned = totalReturned;
        this.totalBroken = totalBroken;
        this.outstanding = totalIssued - totalReturned - totalBroken;
        this.lastTransactionAt = lastTransactionAt;
    }

    // ==================== Getters & Setters ====================

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getBottleType() { return bottleType; }
    public void setBottleType(String bottleType) { this.bottleType = bottleType; }

    public int getTotalIssued() { return totalIssued; }
    public void setTotalIssued(int totalIssued) { this.totalIssued = totalIssued; }

    public int getTotalReturned() { return totalReturned; }
    public void setTotalReturned(int totalReturned) { this.totalReturned = totalReturned; }

    public int getTotalBroken() { return totalBroken; }
    public void setTotalBroken(int totalBroken) { this.totalBroken = totalBroken; }

    public int getOutstanding() { return outstanding; }
    public void setOutstanding(int outstanding) { this.outstanding = outstanding; }

    public Date getLastTransactionAt() { return lastTransactionAt; }
    public void setLastTransactionAt(Date lastTransactionAt) { this.lastTransactionAt = lastTransactionAt; }

    // ==================== Builder ====================

    public static class Builder {
        private String customerId;
        private String bottleType;
        private int totalIssued;
        private int totalReturned;
        private int totalBroken;
        private Date lastTransactionAt;

        public Builder(String customerId, String bottleType) {
            this.customerId = customerId;
            this.bottleType = bottleType;
        }

        public Builder addIssued(int qty) { this.totalIssued += qty; return this; }
        public Builder addReturned(int qty) { this.totalReturned += qty; return this; }
        public Builder addBroken(int qty) { this.totalBroken += qty; return this; }
        public Builder withLastTransactionAt(Date d) { this.lastTransactionAt = d; return this; }

        public String getBottleType() { return bottleType; }

        public BottleLedgerEntry build() {
            return new BottleLedgerEntry(
                customerId, bottleType,
                totalIssued, totalReturned,
                totalBroken, lastTransactionAt);
        }
    }
}

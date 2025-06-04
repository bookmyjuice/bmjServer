package com.bookmyjuice.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_price_entity")
public class ItemPriceEntity {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "item_id", referencedColumnName = "id")
    private ItemEntity item;

    private String name;
    private String description;
    private String externalName;
    private String pricingModel;
    private BigDecimal price;
    private String currencyCode;
    private String status;
    private String periodUnit;
    private Integer period;
    private Boolean trialAvailable;
    private Integer trialPeriod;
    private String trialPeriodUnit;
    private Boolean freeQuantityInDecimal;
    private String invoiceNotes;
    private String taxProfileId;
    private String taxProfileName;
    private String taxProfileType;
    private String accountingCode;
    private String accountingCategory1;
    private String accountingCategory2;
    private String accountingCategory3;
    private String accountingCategory4;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Add more fields as needed from Chargebee ItemPrice API

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ItemEntity getItem() { return item; }
    public void setItem(ItemEntity item) { this.item = item; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExternalName() { return externalName; }
    public void setExternalName(String externalName) { this.externalName = externalName; }
    public String getPricingModel() { return pricingModel; }
    public void setPricingModel(String pricingModel) { this.pricingModel = pricingModel; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPeriodUnit() { return periodUnit; }
    public void setPeriodUnit(String periodUnit) { this.periodUnit = periodUnit; }
    public Integer getPeriod() { return period; }
    public void setPeriod(Integer period) { this.period = period; }
    public Boolean getTrialAvailable() { return trialAvailable; }
    public void setTrialAvailable(Boolean trialAvailable) { this.trialAvailable = trialAvailable; }
    public Integer getTrialPeriod() { return trialPeriod; }
    public void setTrialPeriod(Integer trialPeriod) { this.trialPeriod = trialPeriod; }
    public String getTrialPeriodUnit() { return trialPeriodUnit; }
    public void setTrialPeriodUnit(String trialPeriodUnit) { this.trialPeriodUnit = trialPeriodUnit; }
    public Boolean getFreeQuantityInDecimal() { return freeQuantityInDecimal; }
    public void setFreeQuantityInDecimal(Boolean freeQuantityInDecimal) { this.freeQuantityInDecimal = freeQuantityInDecimal; }
    public String getInvoiceNotes() { return invoiceNotes; }
    public void setInvoiceNotes(String invoiceNotes) { this.invoiceNotes = invoiceNotes; }
    public String getTaxProfileId() { return taxProfileId; }
    public void setTaxProfileId(String taxProfileId) { this.taxProfileId = taxProfileId; }
    public String getTaxProfileName() { return taxProfileName; }
    public void setTaxProfileName(String taxProfileName) { this.taxProfileName = taxProfileName; }
    public String getTaxProfileType() { return taxProfileType; }
    public void setTaxProfileType(String taxProfileType) { this.taxProfileType = taxProfileType; }
    public String getAccountingCode() { return accountingCode; }
    public void setAccountingCode(String accountingCode) { this.accountingCode = accountingCode; }
    public String getAccountingCategory1() { return accountingCategory1; }
    public void setAccountingCategory1(String accountingCategory1) { this.accountingCategory1 = accountingCategory1; }
    public String getAccountingCategory2() { return accountingCategory2; }
    public void setAccountingCategory2(String accountingCategory2) { this.accountingCategory2 = accountingCategory2; }
    public String getAccountingCategory3() { return accountingCategory3; }
    public void setAccountingCategory3(String accountingCategory3) { this.accountingCategory3 = accountingCategory3; }
    public String getAccountingCategory4() { return accountingCategory4; }
    public void setAccountingCategory4(String accountingCategory4) { this.accountingCategory4 = accountingCategory4; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

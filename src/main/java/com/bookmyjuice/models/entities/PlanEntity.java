package com.bookmyjuice.models.entities;

import java.math.BigDecimal;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan_entity")
public class PlanEntity {

    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String currencyCode;
    private String periodUnit;
    private Integer period;
    private String status;
    @Nullable
    private Boolean enabledInPortal; // optional
    @Nullable
    private Boolean enabledForCheckout; // optional
    @Nullable
    private String invoiceNotes; // optional
    @Nullable
    private String metaData; // optional
    @Nullable
    private String externalName; // optional
    @Nullable
    private Boolean showDescriptionInInvoices; // optional
    @Nullable
    private Boolean showDescriptionInQuotes; // optional
    @Nullable
    private Boolean archived; // optional
    // Add more fields as needed from Chargebee Plan API

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getPeriodUnit() {
        return periodUnit;
    }

    public void setPeriodUnit(String periodUnit) {
        this.periodUnit = periodUnit;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getEnabledInPortal() {
        return enabledInPortal;
    }

    public void setEnabledInPortal(Boolean enabledInPortal) {
        this.enabledInPortal = enabledInPortal;
    }

    public Boolean getEnabledForCheckout() {
        return enabledForCheckout;
    }

    public void setEnabledForCheckout(Boolean enabledForCheckout) {
        this.enabledForCheckout = enabledForCheckout;
    }

    public String getInvoiceNotes() {
        return invoiceNotes;
    }

    public void setInvoiceNotes(String invoiceNotes) {
        this.invoiceNotes = invoiceNotes;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public String getExternalName() {
        return externalName;
    }

    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }

    public Boolean getShowDescriptionInInvoices() {
        return showDescriptionInInvoices;
    }

    public void setShowDescriptionInInvoices(Boolean showDescriptionInInvoices) {
        this.showDescriptionInInvoices = showDescriptionInInvoices;
    }

    public Boolean getShowDescriptionInQuotes() {
        return showDescriptionInQuotes;
    }

    public void setShowDescriptionInQuotes(Boolean showDescriptionInQuotes) {
        this.showDescriptionInQuotes = showDescriptionInQuotes;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }
}

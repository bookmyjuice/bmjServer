package com.bookmyjuice.models;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "CustomerEntity")
public class CustomerEntity {
    @Id 
    @Column(name = "customer_id") // Map the id field to the customer_id column in subscription_entity table
    private String Id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String company;
    private String autoCollection;
    private int netTermDays;
    private boolean allowDirectDebit;
    private long createdAt;
    private String taxability;
    private long updatedAt;
    private String piiCleared;
    private String channel;
    private long resourceVersion;
    private boolean deleted;
    // private String cardStatus;
    private Long promotionalCredits;
    private Long refundableCredits;
    private Long excessPayments;
    private Long unbilledCharges;
    private String preferredCurrencyCode;
    // private int mrr;
    private String primaryPaymentSourceId;

    // @OneToOne(cascade = CascadeType.ALL)
    // @JoinColumn(name = "payment_method_id", referencedColumnName = "id")
    // private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<SubscriptionEntity> subscriptions;




    public String getId() {
        return Id;
      
    }































    public void setId(String customerId) {
        this.Id = customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAutoCollection() {
        return autoCollection;
    }

    public void setAutoCollection(String autoCollection) {
        this.autoCollection = autoCollection;
    }

    public int getNetTermDays() {
        return netTermDays;
    }

    public void setNetTermDays(int netTermDays) {
        this.netTermDays = netTermDays;
    }

    public boolean isAllowDirectDebit() {
        return allowDirectDebit;
    }

    public void setAllowDirectDebit(boolean allowDirectDebit) {
        this.allowDirectDebit = allowDirectDebit;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getTaxability() {
        return taxability;
    }

    public void setTaxability(String taxability) {
        this.taxability = taxability;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPiiCleared() {
        return piiCleared;
    }

    public void setPiiCleared(String piiCleared) {
        this.piiCleared = piiCleared;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public long getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(long resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    // public String getCardStatus() {
    //     return cardStatus;
    // }

    // public void setCardStatus(String cardStatus) {
    //     this.cardStatus = cardStatus;
    // }

    public Long getPromotionalCredits() {
        return promotionalCredits;
    }

    public void setPromotionalCredits(Long promotionalCredits) {
        this.promotionalCredits = promotionalCredits;
    }

    public Long getRefundableCredits() {
        return refundableCredits;
    }

    public void setRefundableCredits(Long refundableCredits) {
        this.refundableCredits = refundableCredits;
    }

    public Long getExcessPayments() {
        return excessPayments;
    }

    public void setExcessPayments(Long excessPayments) {
        this.excessPayments = excessPayments;
    }

    public Long getUnbilledCharges() {
        return unbilledCharges;
    }

    public void setUnbilledCharges(Long unbilledCharges) {
        this.unbilledCharges = unbilledCharges;
    }

    public String getPreferredCurrencyCode() {
        return preferredCurrencyCode;
    }

    public void setPreferredCurrencyCode(String preferredCurrencyCode) {
        this.preferredCurrencyCode = preferredCurrencyCode;
    }

    // public int getMrr() {
    //     return mrr;
    // }

    // public void setMrr(int mrr) {
    //     this.mrr = mrr;
    // }

    public String getPrimaryPaymentSourceId() {
        return primaryPaymentSourceId;
    }

    public void setPrimaryPaymentSourceId(String primaryPaymentSourceId) {
        this.primaryPaymentSourceId = primaryPaymentSourceId;
    }

    // public PaymentMethod getPaymentMethod() {
    //     return paymentMethod;
    // }

    // public void setPaymentMethod(PaymentMethod paymentMethod) {
    //     this.paymentMethod = paymentMethod;
    // }

    // public List<SubscriptionEntity> getSubscriptions() {
    //     return subscriptions;
    // }

    // public void setSubscriptions(List<SubscriptionEntity> subscriptions) {
    //     this.subscriptions = subscriptions;
    // }

    public List<SubscriptionEntity> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<SubscriptionEntity> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
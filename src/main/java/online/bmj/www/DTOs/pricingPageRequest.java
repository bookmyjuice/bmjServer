package online.bmj.www.DTOs;

public class pricingPageRequest{
	    private String subscriptionId;
        private Boolean isExistingSubscription;
        private String customerId;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Boolean getIsExistingSubscription() {
        return isExistingSubscription;
    }

    public void setIsExistingSubscription(Boolean isExistingSubscription) {
        this.isExistingSubscription = isExistingSubscription;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    
}
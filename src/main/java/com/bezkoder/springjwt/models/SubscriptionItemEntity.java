// package com.bezkoder.springjwt.models;

// import jakarta.persistence.Entity;
// import jakarta.persistence.Id;
// import jakarta.persistence.JoinColumn;
// import jakarta.persistence.ManyToOne;
// import jakarta.persistence.Table;

// @Entity
// @Table(name = "subscription_item_entity")
// public class SubscriptionItemEntity {
//     @Id
//     private String itemPriceId;

//     private String itemType;
//     private int quantity;
//     private int unitPrice;
//     private int amount;
//     private int freeQuantity;
//     private String object;

//     @ManyToOne
//     @JoinColumn(name = "subscription_id", referencedColumnName = "id")
//     private SubscriptionEntity subscription;


//     // Getters and setters
  

//     public String getItemPriceId() {
//         return itemPriceId;
//     }

//     public void setItemPriceId(String itemPriceId) {
//         this.itemPriceId = itemPriceId;
//     }

//     public String getItemType() {
//         return itemType;
//     }

//     public void setItemType(String itemType) {
//         this.itemType = itemType;
//     }

//     public int getQuantity() {
//         return quantity;
//     }

//     public void setQuantity(int quantity) {
//         this.quantity = quantity;
//     }

//     public int getUnitPrice() {
//         return unitPrice;
//     }

//     public void setUnitPrice(int unitPrice) {
//         this.unitPrice = unitPrice;
//     }

//     public int getAmount() {
//         return amount;
//     }

//     public void setAmount(int amount) {
//         this.amount = amount;
//     }

//     public int getFreeQuantity() {
//         return freeQuantity;
//     }

//     public void setFreeQuantity(int freeQuantity) {
//         this.freeQuantity = freeQuantity;
//     }

//     public String getObject() {
//         return object;
//     }

//     public void setObject(String object) {
//         this.object = object;
//     }

//     public SubscriptionEntity getSubscription() {
//         return subscription;
//     }

//     public void setSubscription(SubscriptionEntity subscription) {
//         this.subscription = subscription;
//     }
// }

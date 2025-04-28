// package com.bezkoder.springjwt.models;

// import jakarta.persistence.Entity;
// import jakarta.persistence.Id;
// import jakarta.persistence.OneToOne;
// import jakarta.persistence.Table;

// @Entity
// @Table(name = "PaymentMethod")
// public class PaymentMethod {
//     @Id
//     private String referenceId;

//     private String object;
//     private String type;
//     private String gateway;
//     private String gatewayAccountId;
//     private String status;

//     @OneToOne(mappedBy = "paymentMethod")
//     private CustomerEntity customer;

//     public CustomerEntity getCustomer() {
//         return customer;
//     }

//     public void setCustomer(CustomerEntity customer) {
//         this.customer = customer;
//     }

//     public String getGatewayAccountId() {
//         return gatewayAccountId;
//     }

//     public void setGatewayAccountId(String gatewayAccountId) {
//         this.gatewayAccountId = gatewayAccountId;
//     }

//     public String getObject() {
//         return object;
//     }

//     public void setObject(String object) {
//         this.object = object;
//     }

//     public String getType() {
//         return type;
//     }

//     public void setType(String type) {
//         this.type = type;
//     }

//     public String getStatus() {
//         return status;
//     }

//     public void setStatus(String status) {
//         this.status = status;
//     }

//     public String getReferenceId() {
//         return referenceId;
//     }

//     public void setReferenceId(String referenceId) {
//         this.referenceId = referenceId;
//     }

//     public String getGateway() {
//         return gateway;
//     }

//     public void setGateway(String gateway) {
//         this.gateway = gateway;
//     }
// }

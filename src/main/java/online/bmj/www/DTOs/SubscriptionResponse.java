// package online.bmj.www.DTOs;
// // SubscriptionResponse.java

// import java.time.LocalDateTime;
// import java.util.UUID;

// import online.bmj.www.entities.SubscriptionEntity;

// public record SubscriptionResponse(
//     UUID id,
//     String planId,
//     String status,
//     LocalDateTime nextBillingDate
// ) {
//     public SubscriptionResponse(UUID id, String planId, String status, LocalDateTime nextBillingDate) {
// 		this.planId = planId;
//     	this.id=id;
//     	this.status=status;
//     	this.nextBillingDate=nextBillingDate;
    	
    	
// 	}

// 	public static SubscriptionResponse fromEntity(SubscriptionEntity subscription) {
//         return new SubscriptionResponse(
//             subscription.getId(),
//             subscription.getPlanId(),
//             subscription.getStatus().name(),
//             subscription.getNextBillingDate()
//         );
//     }
// }
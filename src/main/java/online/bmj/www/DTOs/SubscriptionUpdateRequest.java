package online.bmj.www.DTOs;

import java.util.Map;

// import jakarta.persistence.Entity;


public record SubscriptionUpdateRequest(
	    String newPlanId,
	    Integer billingCycles,
	    Map<String, String> addons
	) {}
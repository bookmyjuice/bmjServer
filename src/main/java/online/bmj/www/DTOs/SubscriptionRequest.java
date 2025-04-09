package online.bmj.www.DTOs;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(
   @NotBlank(message = "Customer ID must not be blank") String customerId,
   @NotBlank(message = "Plan ID must not be blank") String planId
) {}
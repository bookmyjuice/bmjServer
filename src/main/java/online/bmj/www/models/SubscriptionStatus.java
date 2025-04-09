package online.bmj.www.models;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import lombok.extern.java.Log;

public enum SubscriptionStatus {
    ACTIVE("active"),
    FUTURE("future"),
    IN_TRIAL("in_trial"),
    NON_RENEWING("non_renewing"),
    CANCELLED("cancelled"),
    PAUSED("paused"),
    SCHEDULED_FOR_CANCELLATION("scheduled_for_cancellation"),
    REACTIVATED("reactivated"),
    INCOMPLETE("incomplete"),
    UNKNOWN("unknown");
	private static final Logger log = LoggerFactory.getLogger(SubscriptionStatus.class);

    private final String chargebeeValue;

    SubscriptionStatus(String chargebeeValue) {
        this.chargebeeValue = chargebeeValue;
    }

    public String getChargebeeValue() {
        return chargebeeValue;
    }

    public static SubscriptionStatus fromChargebeeValue(String value) {
        return Arrays.stream(values())
            .filter(status -> status.chargebeeValue.equals(value.toUpperCase()))
            .findFirst()
            .orElseGet(() -> {
                // Log unknown status for investigation
                log.warn("Unknown Chargebee subscription status: {}", value);
                return UNKNOWN;
            });
    }
}
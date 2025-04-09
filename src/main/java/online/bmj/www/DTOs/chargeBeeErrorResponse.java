package online.bmj.www.DTOs;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.ResponseBody;

import online.bmj.www.Exceptions.ChargebeeException;
import online.bmj.www.Exceptions.RateLimitExceededException;

@ResponseBody
public record chargeBeeErrorResponse(
	    String code,
	    String message,
	    Map<String, Object> details
	) {
	    public chargeBeeErrorResponse(ChargebeeException ex) {
	        this(
	            ex.getErrorCode(),
	            ex.getMessage(),
	            buildDetails(ex)
	        );
	    }

	    private static Map<String, Object> buildDetails(ChargebeeException ex) {
	        Map<String, Object> details = new HashMap<>();
	        if (ex instanceof RateLimitExceededException rateEx) {
	            details.put("retry_after_seconds", rateEx.getRetryAfterSeconds());
	        }
	        return details;
	    }

		public Map<String, Object> addDetail(String string, int retryAfterSeconds) {
			Map<String, Object> newDetails = new HashMap<>();
//	        if (ex instanceof RateLimitExceededException rateEx) {
				newDetails.put(string, retryAfterSeconds);
//	        }
			return newDetails;
		}
	}
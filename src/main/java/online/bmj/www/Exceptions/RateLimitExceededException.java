package online.bmj.www.Exceptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS) // 429
public class RateLimitExceededException extends ChargebeeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6005814854396220743L;
	private final int retryAfterSeconds;
    
    public RateLimitExceededException(int retryAfterSeconds) {
        super("RATE_LIMIT_EXCEEDED", 
             "API rate limit exceeded. Retry after " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
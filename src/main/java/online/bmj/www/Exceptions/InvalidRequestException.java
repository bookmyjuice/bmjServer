package online.bmj.www.Exceptions;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends ChargebeeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 5489764236292629217L;

	public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super("INVALID_REQUEST", message, cause);
    }
}
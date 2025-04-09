package online.bmj.www.Exceptions;

public abstract class ChargebeeException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String errorCode;
    
    public ChargebeeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ChargebeeException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

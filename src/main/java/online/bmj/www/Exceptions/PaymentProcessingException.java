package online.bmj.www.Exceptions;

public class PaymentProcessingException extends ChargebeeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3347911825595346377L;

	public PaymentProcessingException(String message) {
        super("PAYMENT_FAILURE", message);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super("PAYMENT_FAILURE", message, cause);
    }
}
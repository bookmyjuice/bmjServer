package online.bmj.www.DTOs;
import jakarta.validation.constraints.NotBlank;
import online.bmj.www.models.User;

public class SignupRequest {

	@NotBlank
	public User user;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}

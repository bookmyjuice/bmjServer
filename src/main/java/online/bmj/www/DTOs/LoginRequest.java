package online.bmj.www.DTOs;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
	@NotBlank
	private String phone;

//	@NotBlank
//	private String email;

	@NotBlank
	private String password;

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
	
//	public String getEmail() {
//		return email;
//	}
//	public void setEmail(String email) {
//		this.email = email;
//	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}

package com.bookmyjuice.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Password reset via email verification code.
 * User provides email, verification code, and new password.
 */
public class ResetPasswordEmailRequest {

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    @NotBlank
    @Size(min = 6, max = 6)
    private String verificationCode;

    @NotBlank
    @Size(min = 8, max = 40)
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

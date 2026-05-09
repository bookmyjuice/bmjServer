package com.bookmyjuice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Password reset via mobile OTP.
 * User provides phone, OTP, and new password.
 */
public class ResetPasswordMobileRequest {

    @NotBlank
    @Size(max = 20)
    private String phone;

    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;

    @NotBlank
    @Size(min = 8, max = 40)
    private String password;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

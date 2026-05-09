package com.bookmyjuice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to link Google account to existing user via phone OTP.
 */
public class LinkGoogleAccountRequest {

    @NotBlank
    @Size(max = 20)
    private String phone;

    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;

    @NotBlank
    private String googleId;

    private String photoUrl;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}

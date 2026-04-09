package com.bookmyjuice.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SendOTPRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    public SendOTPRequest() {
    }

    public SendOTPRequest(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

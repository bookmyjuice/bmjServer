package com.bookmyjuice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for passwordless Google-first signup (A-1).
 *
 * The Google identity is re-verified from {@code idToken}; the phone number is
 * the subject of {@code signupToken}. Neither is trusted from the client body.
 */
public class GoogleSignupRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    @NotBlank(message = "Phone verification token is required")
    private String signupToken;

    /** Optional: blank means the customer skipped the address step. */
    @Size(max = 120, message = "Address must not exceed 120 characters")
    private String address;

    @Size(max = 120, message = "Extended address must not exceed 120 characters")
    private String extendedAddr;

    @Size(max = 120, message = "Extended address 2 must not exceed 120 characters")
    private String extendedAddr2;

    @Size(max = 120, message = "City must not exceed 120 characters")
    private String city;

    @Size(max = 120, message = "State must not exceed 120 characters")
    private String state;

    @Size(max = 10, message = "ZIP code must not exceed 10 characters")
    private String zip;

    @Size(max = 2, message = "Country must be 2-letter code")
    private String country;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getSignupToken() {
        return signupToken;
    }

    public void setSignupToken(String signupToken) {
        this.signupToken = signupToken;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getExtendedAddr() {
        return extendedAddr;
    }

    public void setExtendedAddr(String extendedAddr) {
        this.extendedAddr = extendedAddr;
    }

    public String getExtendedAddr2() {
        return extendedAddr2;
    }

    public void setExtendedAddr2(String extendedAddr2) {
        this.extendedAddr2 = extendedAddr2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}

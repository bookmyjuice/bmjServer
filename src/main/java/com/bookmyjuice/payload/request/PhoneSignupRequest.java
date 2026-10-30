package com.bookmyjuice.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for passwordless phone-first signup (A-1).
 *
 * The phone number is not supplied by the client: it is the subject of
 * {@code signupToken}, the short-lived proof minted by `/auth/login-otp`
 * after a successful OTP check.
 *
 * Since the mandatory-email redesign, an account also needs a verified email:
 * {@code email} must match the subject of {@code emailVerificationToken}, the
 * proof minted by `/auth/verify-email-code`. The delivery address is optional
 * at signup ("Skip for now") and can be added later from the profile.
 */
public class PhoneSignupRequest {

    @NotBlank(message = "Phone verification token is required")
    private String signupToken;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    /** Proof that {email} passed the email OTP check; matches its subject. */
    @NotBlank(message = "Email verification token is required")
    private String emailVerificationToken;

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

    public String getSignupToken() {
        return signupToken;
    }

    public void setSignupToken(String signupToken) {
        this.signupToken = signupToken;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
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

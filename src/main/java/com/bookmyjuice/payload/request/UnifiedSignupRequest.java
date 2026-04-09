package com.bookmyjuice.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for unified signup with email, phone, and address
 */
public class UnifiedSignupRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 40, message = "Password must be between 8 and 40 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Address is required")
    @Size(max = 120, message = "Address must not exceed 120 characters")
    private String address;

    @Size(max = 120, message = "Extended address must not exceed 120 characters")
    private String extendedAddr;

    @Size(max = 120, message = "Extended address 2 must not exceed 120 characters")
    private String extendedAddr2;

    @NotBlank(message = "City is required")
    @Size(max = 120, message = "City must not exceed 120 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 120, message = "State must not exceed 120 characters")
    private String state;

    @NotBlank(message = "ZIP code is required")
    @Size(max = 10, message = "ZIP code must not exceed 10 characters")
    private String zip;

    @NotBlank(message = "Country is required")
    @Size(max = 2, message = "Country must be 2-letter code")
    private String country;

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

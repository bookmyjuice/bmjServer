package com.bookmyjuice.payload.request;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank
    @Size(min = 3, max = 10)
    @Pattern(regexp = "^[0-9]*$", message = "must be a number")
    private String username;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    private Set<String> role;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    @Size(max = 25)
    private String firstName;
    @Size(max = 25)
    private String lastName;

    @Size(max = 120)
    private String address;

    @Size(max = 120)
    private String extendedAddr;
    @Size(max = 120)
    private String extendedAddr2;

    @Size(max = 120)
    private String city;
    @Size(max = 120)
    private String state;
    @Size(max = 2)
    private String country;
    @Size(max = 6)
    private String zip;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRole() {
        return this.role;
    }

    public void setRole(Set<String> role) {
        this.role = role;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
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
}

package com.bookmyjuice.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users", uniqueConstraints = {
		@UniqueConstraint(columnNames = "username"),
		@UniqueConstraint(columnNames = "email")
})
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
	@jakarta.persistence.SequenceGenerator(name = "user_seq", sequenceName = "user_seq", initialValue = 500, allocationSize = 1)
	private Long id;

	@NotBlank
	@Size(max = 20)
	private String username;

	@NotBlank
	@Size(max = 50)
	@Email
	private String email;

	@NotBlank
	@Size(max = 120)
	private String password;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<com.bookmyjuice.models.Role> roles = new HashSet<>();

	@Size(max = 120)
	private String address;

	@Size(max = 120)
	private String extendedAddr;
	@Size(max = 25)
	private String firstName;
	@Size(max = 25)
	private String lastName;
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

	@Size(max = 20)
	private String phone;

	/**
	 * Chargebee customer ID - links local user to Chargebee customer
	 * Relationship: users.chargebee_customer_id = customers.id (1:1)
	 */
	@Column(name = "chargebee_customer_id", unique = true)
	private String chargebeeCustomerId;

	/**
	 * Google Photo URL - populated when user signs up via Google
	 * Read-only field (cannot be edited by user)
	 */
	@Column(name = "google_photo_url", length = 500)
	private String googlePhotoUrl;

	/**
	 * Google ID - unique identifier from Google account
	 * Used for Google sign-in authentication
	 */
	@Column(name = "google_id", unique = true)
	private String googleId;

	public User() {
	}

	public User(String username, String email, String password) {
		this.username = username;
		this.email = email;
		this.password = password;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public Set<com.bookmyjuice.models.Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<com.bookmyjuice.models.Role> roles) {
		this.roles = roles;
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

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getChargebeeCustomerId() {
		return chargebeeCustomerId;
	}

	public void setChargebeeCustomerId(String chargebeeCustomerId) {
		this.chargebeeCustomerId = chargebeeCustomerId;
	}

	public String getGooglePhotoUrl() {
		return googlePhotoUrl;
	}

	public void setGooglePhotoUrl(String googlePhotoUrl) {
		this.googlePhotoUrl = googlePhotoUrl;
	}

	public String getGoogleId() {
		return googleId;
	}

	public void setGoogleId(String googleId) {
		this.googleId = googleId;
	}

}

package online.bmj.www.models;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.repository.query.Param;

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
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(columnNames = "phone"),
		@UniqueConstraint(columnNames = "email") })
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 20)
	private String phone;

	@NotBlank
	@Size(max = 50)
	@Email
	private String email;

	@NotBlank
	@Size(max = 120)
	private String password;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

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

	public User() {
	}

	public User(String phone, String email, String password) {
		this.phone = phone;
		this.email = email;
		this.password = password;
	}

	public User(@Param("email") String email, @Param("phone") String phone, @Param("password") String password,
			@Param("address") String address, @Param("firstName") String firstName, @Param("lastName") String lastName,
			@Param("city") String city, @Param("extendedAdrress") String extendedAddr,
			@Param("extendedAdrress2") String extendedAddr2, @Param("state") String state,
			@Param("country") String country) {

		this.email = email;
		this.phone = phone;
		this.password = password;

		this.firstName = firstName;
		this.lastName = lastName;
		this.address = address;
		this.extendedAddr = extendedAddr;
		this.extendedAddr2 = extendedAddr2;
		this.city = city;
		this.state = state;
		this.country = country;
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
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

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}
}

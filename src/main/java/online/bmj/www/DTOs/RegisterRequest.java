package online.bmj.www.DTOs;

public class RegisterRequest {
	private String id;
	private String firstName;
	private String lastName;
	private String phone;
	private String email;
	private String addr;
	private String extendedAddr;
	private String extendedAddr2;
	private String city;
	private String state;
	private String country;
	private String zip;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
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
	public String getAddr() {
		return addr;
	}
	public void setAddr(String addr) {
		this.addr = addr;
	}
	public String getExteendedAddr() {
		return extendedAddr;
	}
	public void setExteendedAddr(String exteendedAddr) {
		this.extendedAddr = exteendedAddr;
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

}

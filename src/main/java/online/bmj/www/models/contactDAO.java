package online.bmj.www.models;

public class contactDAO {
	
	public contactDAO(String id, String phone, String firstName, String lastName, String email) {
//		super();
		this.id = id;
		this.phone = phone;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}
	public String id;
	public String phone;
	public String firstName;
	public String lastName;
	public String email;
	
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
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

}

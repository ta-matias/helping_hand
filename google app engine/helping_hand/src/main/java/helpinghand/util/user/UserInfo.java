package helpinghand.util.user;

public class UserInfo {
	
	public String phone;
	public String address1;
	public String address2;
	public String city;
	public String zip;
	
	public UserInfo() {}

	public UserInfo(String phone, String address1, String address2, String city, String zip) {
		this.phone = phone;
		this.address1 = address1;
		this.address2 = address2;
		this.city = city;
		this.zip = zip;
	}
	
	public boolean validate() {
		if(phone == null) return false;
		if(address1 == null) return false;
		if(address2 == null) return false;
		if(city == null) return false;
		if(zip == null)return false;
		
		return true;
	}
	
}

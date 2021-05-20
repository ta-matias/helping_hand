package helpinghand.util.inst;

public class InstDataPublic {

	public String name;
	public String initials;
	public String phone;
	public String address;
	public String addressComp;
	public String location;
	

	public InstDataPublic() {}
	
	public InstDataPublic(String name, String initials, String address, String addressComp, String location, String phone) {
		this.name = name;
		this.initials = initials;
		this.address = address;
		this.addressComp = addressComp;
		this.location = location;	
		this.phone = phone;
	}
}

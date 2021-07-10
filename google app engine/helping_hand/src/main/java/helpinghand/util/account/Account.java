package helpinghand.util.account;

public class Account {
	
	
	public String id;
	public String email;
	public String creation;
	public boolean status;
	public boolean visibility;
	
	public Account() {}

	public Account(String id, String email,String creation, boolean status, boolean visibility) {
		this.id = id;
		this.email = email;
		this.creation = creation;
		this.status = status;
		this.visibility = visibility;
	}
	
	
}
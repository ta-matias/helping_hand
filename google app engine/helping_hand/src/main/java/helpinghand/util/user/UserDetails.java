package helpinghand.util.user;

public class UserDetails {

	public String userId;
	public String email;
	public boolean status;
	public String role;
	public String creation;
	
	public UserDetails() {}

	public UserDetails(String userId, String email, boolean status, String role, String creation) {
		this.userId = userId;
		this.email = email;
		this.status = status;
		this.role = role;
		this.creation = creation;
	}

}

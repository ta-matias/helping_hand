package helpinghand.util.account;

public class UserProfile {
	
	public String name;
	public String bio;
	
	public UserProfile() {}
	
	public UserProfile(String name, String bio) {
		this.name = name;
		this.bio = bio;
	}
	
	public boolean badData() {
		if(name == null) 
			return true;
		if(bio == null) 
			return true;
		return false;
	}
	
}

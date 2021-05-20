package helpinghand.util.user;

public class UserProfile {
	
	public boolean publicProfile;
	public String name;
	public String bio;
	
	public UserProfile() {}

	public UserProfile(boolean publicProfile, String name, String bio) {
		this.publicProfile = publicProfile;
		this.name = name;
		this.bio = bio;
	}
	
	public boolean validate() {
		if(name == null) return false;
		if(bio == null) return false;
		return true;
	}
	
	
}

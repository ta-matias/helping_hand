package helpinghand.util.account;

import com.google.cloud.datastore.Entity;

import static helpinghand.util.account.AccountUtils.PROFILE_NAME_PROPERTY;
import static helpinghand.util.account.AccountUtils.PROFILE_BIO_PROPERTY;

public class UserProfile {
	
	public String name;
	public String bio;
	
	public UserProfile() {}
	
	public UserProfile(Entity profile) {
		this.name = profile.getString(PROFILE_NAME_PROPERTY);
		this.bio = profile.getString(PROFILE_BIO_PROPERTY);
	}
	
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

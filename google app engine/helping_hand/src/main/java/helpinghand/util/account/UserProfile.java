package helpinghand.util.account;

import com.google.cloud.datastore.Entity;

import static helpinghand.util.account.AccountUtils.PROFILE_AVATAR_PROPERTY;
import static helpinghand.util.account.AccountUtils.PROFILE_NAME_PROPERTY;
import static helpinghand.util.account.AccountUtils.PROFILE_BIO_PROPERTY;

public class UserProfile {
	
	public boolean visibility;
	public String avatar;
	public String name;
	public String bio;
	
	public UserProfile() {}
	
	public UserProfile(boolean visibility,Entity profile) {
		this.visibility = visibility;
		this.avatar = profile.getString(PROFILE_AVATAR_PROPERTY);
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

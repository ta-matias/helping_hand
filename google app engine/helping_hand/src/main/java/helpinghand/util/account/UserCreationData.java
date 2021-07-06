package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.EMAIL_REGEX;
import static helpinghand.util.GeneralUtils.PASSWORD_REGEX;
import static helpinghand.util.GeneralUtils.badString;

public class UserCreationData {
	
	public String id;
	public String email;
	public String password;
	public String confirmation;
	
	public UserCreationData() {}

	public UserCreationData(String id, String email, String password, String confirmation) {
		this.id = id;
		this.email = email;
		this.password = password;
		this.confirmation = confirmation;
	}
	
	public boolean badData() {
		if(badString(id)) 
			return true;
		if(!confirmation.equals(password))
			return true;
		if(!email.matches(EMAIL_REGEX))
			return true;
		if(!password.matches(PASSWORD_REGEX))
			return true;
		return false;
	}
	
}

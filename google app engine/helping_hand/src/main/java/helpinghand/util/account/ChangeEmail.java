package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.EMAIL_REGEX;
import static helpinghand.util.GeneralUtils.badString;

public class ChangeEmail {
	
	public String email;
	public String password;
	
	public ChangeEmail(String email, String password) {
		this.email = email;
		this.password = password;
	}
	
	public boolean badData() {
		if(!email.matches(EMAIL_REGEX))return false;
		if(badString(password))return false;
		return false;
	}
}

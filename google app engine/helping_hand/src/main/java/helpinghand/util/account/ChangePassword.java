package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.PASSWORD_REGEX;

public class ChangePassword {
	
	public String oldPassword, newPassword, confirmation;
	
	public ChangePassword() {}

	public ChangePassword(String oldPassword, String newPassword, String confirmation) {
		this.oldPassword = oldPassword;
		this.newPassword = newPassword;
		this.confirmation = confirmation;
	}
	
	public boolean badData() {
		if(badString(oldPassword)) 
			return true;
		if(newPassword.equals(oldPassword))
			return true;
		if(!confirmation.equals(newPassword))
			return true;
		if(!newPassword.matches(PASSWORD_REGEX))
			return true;
		return false;
	}
	
}

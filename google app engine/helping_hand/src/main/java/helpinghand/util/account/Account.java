package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.EMAIL_REGEX;
import static helpinghand.util.GeneralUtils.badString;

public class Account {
	public String id;
	public String email;
	public boolean status;
	public boolean visible;
	
	public Account(String id, String email, boolean status, boolean visible) {
		this.id = id;
		this.email = email;
		this.status = status;
		this.visible = visible;
	}
	
	public boolean badData() {
		if(badString(id))return true;
		if(!email.matches(EMAIL_REGEX)) return true;
		return false;
	}
}

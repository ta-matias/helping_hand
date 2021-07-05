package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.util.GeneralUtils.PASSWORD_REGEX;

public class Login {
	
	public String id;
	public String password;
	
	public Login() {}
	
	public Login(String id,String password) {
		this.id = id;
		this.password = password;
	}
	
	public boolean badData() {
		if(badString(id))
			return true;
		if(!password.matches(PASSWORD_REGEX))
			return true;
		return false;
	}
	
}

package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.EMAIL_REGEX;
import static helpinghand.util.GeneralUtils.PASSWORD_REGEX;
import static helpinghand.util.GeneralUtils.badString;

public class InstitutionCreationData {
	
	public String id;
	public String name;
	public String initials;
	public String email;
	public String password;
	public String confirmation;
	
	public InstitutionCreationData() {}
	
	//maybe add more things here
	public InstitutionCreationData(String id, String name, String initials, String email, String password, String confirmation) {
		this.id = id;
		this.name = name;
		this.initials = initials;
		this.email = email;
		this.password = password;
		this.confirmation = confirmation;
	}
	
	public boolean badData() {
		if(badString(id)) return true;
		if(badString(name)) return true;
		if(badString(initials))return true;
		if(!password.equals(confirmation)) return true;
		if(!email.matches(EMAIL_REGEX)) return true;
		if(!password.matches(PASSWORD_REGEX)) return true;
		return false;
	}
}
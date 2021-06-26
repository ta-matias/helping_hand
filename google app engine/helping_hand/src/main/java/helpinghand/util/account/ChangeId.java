package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.badString;

public class ChangeId {
	
	public String id;
	public String password;
	public ChangeId(String id, String password) {
		this.id = id;
		this.password = password;
	}
	
	public boolean badData() {
		if(badString(id)) return true;
		if(badString(password)) return true;
		return false;
	}
	
}

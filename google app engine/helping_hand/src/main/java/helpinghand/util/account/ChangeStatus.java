package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.badString;

public class ChangeStatus {
	
	public boolean status;
	public String password;
	
	public ChangeStatus() {}
	
	public ChangeStatus(boolean status, String password) {
		super();
		this.status = status;
		this.password = password;
	}
	
	public boolean badData() {
		if(badString(password))return true;
		return false;
	}
}

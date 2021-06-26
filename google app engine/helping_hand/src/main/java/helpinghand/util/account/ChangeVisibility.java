package helpinghand.util.account;

import static helpinghand.util.GeneralUtils.badString;

public class ChangeVisibility {
	
	public boolean visibility;
	public String password;
	public ChangeVisibility(boolean visibility, String password) {
		super();
		this.visibility = visibility;
		this.password = password;
	}

	public boolean badData() {
		if(badString(password)) return true;
		return false;
	}
}


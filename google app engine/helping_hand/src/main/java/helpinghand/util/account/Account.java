package helpinghand.util.account;

import com.google.cloud.datastore.Entity;

import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_EMAIL_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_CREATION_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_STATUS_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_VISIBILITY_PROPERTY;

public class Account {
	
	
	public String id;
	public String email;
	public String creation;
	public boolean status;
	public boolean visibility;
	
	public Account() {}
	
	public Account(Entity account,boolean ignoreVisibility) {
		this.id = account.getString(ACCOUNT_ID_PROPERTY);
		this.status = account.getBoolean(ACCOUNT_STATUS_PROPERTY);
		this.creation = account.getTimestamp(ACCOUNT_CREATION_PROPERTY).toString();
		this.visibility = account.getBoolean(ACCOUNT_VISIBILITY_PROPERTY);
		this.email = "*****";
		if(ignoreVisibility || this.visibility) {
			this.email = account.getString(ACCOUNT_EMAIL_PROPERTY);		
		}
	}
	
	public Account(String id, String email,String creation, boolean status, boolean visibility) {
		this.id = id;
		this.email = email;
		this.creation = creation;
		this.status = status;
		this.visibility = visibility;
	}
	
	
}
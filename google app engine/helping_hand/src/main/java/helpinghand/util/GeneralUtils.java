package helpinghand.util;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Entity;

import helpinghand.util.account.AccountUtils;

public class GeneralUtils {
	
	public static final String EMAIL_REGEX = "^([_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{2,6}))?$";
	public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
	public static final String TOKEN_NOT_FOUND_ERROR = "Token [%s] does not exist";
	public static final String TOKEN_ACCESS_INSUFFICIENT_ERROR = "Token [%s] cannot execute the operation with current access level ([%d] < [%d])";
	
	public static boolean badString(String s) {
		if(s == null) return true;
		if(s.isBlank()) return true;
		return false;
	}
	
	public static boolean badPassword(Entity account, String password) {
		if(badString(password))return true;
		String accountPassword = account.getString(AccountUtils.ACCOUNT_PASSWORD_PROPERTY);
		String hexPassword = DigestUtils.sha512Hex(password);
		if(accountPassword.equals(hexPassword))return false;
		return true;
	}
	
}

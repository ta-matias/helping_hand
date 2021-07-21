/**
 * 
 */
package helpinghand.util;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Entity;

import helpinghand.util.account.AccountUtils;
/**
 * @author PogChamp Software
 *
 */
public class GeneralUtils {
	
	public static final String EMAIL_REGEX = "^([_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{2,6}))?$";
	public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
	public static final String TOKEN_NOT_FOUND_ERROR = "Token (%d) does not exist";
	public static final String TOKEN_ACCESS_INSUFFICIENT_ERROR = "Token (%d) cannot execute the operation with current access level ((%d) < (%d))";
	public static final String TOKEN_OWNER_ERROR = "Token(%d) does not belong to [%s]";
	public static final String NOTIFICATION_ERROR = "Error notifying user (%d)";
	
	public static final String EMAIL_API_KEY = "EMAIL_API_KEY";
	public static final String EMAIL_API_KEY_NOT_FOUND_ERROR = "Email API key was not found";
	public static final String EMAIL_SENDING_OK ="Successfuly sent email to [%s]";
	public static final String EMAIL_SENDING_ERROR = "Error sending email to [%s]";
	public static final String OUR_EMAIL = "pogchampsoftware@gmail.com";
	public static final String OUR_REST_URL = "www.thehelpinghand.ew.r.appspot.com/rest";
	
	public static final String APP_SECRET_KIND = "Secret";
	public static final String APP_SECRET_VALUE_PROPERTY = "value";
	
	public static final String AVATAR_0 ="https://storage.googleapis.com/thehelpinghand.appspot.com/avatars/avatar_0.png";
	public static final String AVATAR_1 ="https://storage.googleapis.com/thehelpinghand.appspot.com/avatars/avatar_1.png";
	public static final String AVATAR_2 ="https://storage.googleapis.com/thehelpinghand.appspot.com/avatars/avatar_2.png";
	public static final String AVATAR_3 ="https://storage.googleapis.com/thehelpinghand.appspot.com/avatars/avatar_3.png";
	public static final String AVATAR_4 ="https://storage.googleapis.com/thehelpinghand.appspot.com/avatars/avatar_4.png";
	public static final String AVATAR_5 ="https://storage.googleapis.com/thehelpinghand.appspot.com/avatars/avatar_5.png";
	public static final String AVATAR_6 ="https://storage.googleapis.com/thehelpinghand.appspot.com/avatars/avatar_6.png";
	
	
	
	public static boolean badString(String s) {
		if(s == null) 
			return true;
		if(s.isEmpty()) 
			return true;
		return false;
	}
	
	public static boolean badPassword(Entity account, String password) {
		if(badString(password))
			return true;
		
		String accountPassword = account.getString(AccountUtils.ACCOUNT_PASSWORD_PROPERTY);
		String hexPassword = DigestUtils.sha512Hex(password);
		
		if(accountPassword.equals(hexPassword))
			return false;
		
		return true;
	}
	
}

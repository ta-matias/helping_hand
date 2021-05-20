package helpinghand.util.user;

public class UserRegister {
	
	public String userId;
	public String email;
	public String password;
	public String confPassword;
	
	private final String EMAIL_REGEX = "^([_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{2,6}))?$";
	private final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
	
	public UserRegister() {}

	public UserRegister(String userId, String email, String password, String confPassword) {
		this.userId = userId;
		this.email = email;
		this.password = password;
		this.confPassword = confPassword;
	}
	
	public boolean validate() {
		if(userId == null) return false;
		if(!email.matches(EMAIL_REGEX))return false;
		if(!password.matches(PASSWORD_REGEX))return false;
		if(!confPassword.equals(password))return false;
		return true;
	}
	
}

package helpinghand.util;

public class Login {
	public String clientId;
	public String password;
	
	private final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
	
	public Login() {}
	
	public Login(String clientId,String password) {
		this.clientId = clientId;
		this.password = password;
	}
	
	public boolean validate() {
		if(clientId == null)return false;
		if(!password.matches(PASSWORD_REGEX))return false;
		return true;
	}
}

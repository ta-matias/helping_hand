package helpinghand.util.inst;

public class InstData {

	public String name;
	public String initials;
	public String instId;
	public String email;
	public String password;
	public String confPassword;
	
	private final String EMAIL_REGEX = "^([_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{2,6}))?$";
	private final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
	
	public InstData() {}
	
	//maybe add more things here
	public InstData(String name, String initials, String instId, String email, String password, String confPassword) {
		this.name = name;
		this.initials = initials;
		this.instId = instId;
		this.email = email;
		this.password = password;
		this.confPassword = confPassword;
	}
	
	public boolean validate() {
		if(name == null) return false;
		if(initials == null)return false;
		if(instId == null) return false;
		if(!password.equals(confPassword)) return false;
		if(!email.matches(EMAIL_REGEX)) return false;
		if(!password.matches(PASSWORD_REGEX)) return false;
		return true;
	}
}
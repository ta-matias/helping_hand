package helpinghand.util;

public class ChangePass {
	public String oldPassword, newPassword, confirmation;
	
	public ChangePass() {}

	public ChangePass(String oldPassword, String newPassword, String confirmation) {
		this.oldPassword = oldPassword;
		this.newPassword = newPassword;
		this.confirmation = confirmation;
	}
	
	public boolean validate() {
		if(oldPassword == null) return false;
		if(newPassword == null) return false;
		if(newPassword.equals(oldPassword))return false;
		if(!newPassword.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$"))return false;
		if(!confirmation.equals(newPassword))return false;
		return true;
	}
	
	
	
}

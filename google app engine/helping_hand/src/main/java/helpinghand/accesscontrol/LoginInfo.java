package helpinghand.accesscontrol;

public class LoginInfo {
	
	public String name;
	public String role;
	public String tokenId;
	public String expires;
	
	public LoginInfo(String name, String role, String tokenId,String expires) {
		this.name = name;
		this.role = role;
		this.tokenId = tokenId;
		this.expires = expires;
	}
	
	
	
}

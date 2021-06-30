package helpinghand.accesscontrol;

public class LoginInfo {
	
	public String name;
	public String role;
	public long tokenId;
	public String expires;
	
	public LoginInfo(String name, String role, long tokenId,String expires) {
		this.name = name;
		this.role = role;
		this.tokenId = tokenId;
		this.expires = expires;
	}
	
	
	
}

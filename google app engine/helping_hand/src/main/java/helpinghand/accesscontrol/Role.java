package helpinghand.accesscontrol;

public enum Role {
	
	USER(0),
	INSTITUTION(0),
	GBO(1),
	SU(2);
	
	
	
	
	private int access;
	
	Role(int access){
		this.access = access;
	}
	
	public int getAccess() {
		return access;
	}

	
	public static Role getRole(String role_name) {
		for(Role role: Role.values()) {
			if(role.name().equals(role_name)) return role;
		}
		return null;
	}
}

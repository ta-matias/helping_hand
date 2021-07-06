/**
 * 
 */
package helpinghand.accesscontrol;
/**
 * @author PogChamp Software
 *
 */
public enum Role {
	
	ALL(-1),
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
	
	public static Role getRole(String roleName) {
		String normalized = roleName.trim().toUpperCase();
		for(Role role: Role.values())
			if(role.name().equals(normalized)) 
				return role;
		return null;
	}
	
}

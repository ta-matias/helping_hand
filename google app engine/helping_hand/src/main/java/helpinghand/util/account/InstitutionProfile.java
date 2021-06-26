package helpinghand.util.account;

public class InstitutionProfile {
	
	public String name;
	public String initials;
	public String bio;
	public String[] categories;
	
	public InstitutionProfile(String name, String initials, String bio, String[] categories) {
		this.name = name;
		this.initials = initials;
		this.bio = bio;
		this.categories = categories;
	}
	 
	public boolean badData() {
		if(name == null) return true;
		if(initials == null) return true;
		if(bio ==null) return true;
		if(categories == null) return true;
		return false;
	}
	 
}

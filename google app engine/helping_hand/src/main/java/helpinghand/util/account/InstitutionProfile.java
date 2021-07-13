package helpinghand.util.account;

import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Value;



import static helpinghand.util.account.AccountUtils.PROFILE_NAME_PROPERTY;
import static helpinghand.util.account.AccountUtils.PROFILE_BIO_PROPERTY;
import static helpinghand.util.account.AccountUtils.INSTITUTION_PROFILE_INITIALS_PROPERTY;
import static helpinghand.util.account.AccountUtils.INSTITUTION_PROFILE_CATEGORIES_PROPERTY;


public class InstitutionProfile {
	
	public boolean visibility;
	public String name;
	public String initials;
	public String bio;
	public String[] categories;
	
	public InstitutionProfile() {}
	
	public InstitutionProfile(boolean visibility, Entity profile) {
		this.visibility = visibility;
		this.name = profile.getString(PROFILE_NAME_PROPERTY);
		this.initials = profile.getString(INSTITUTION_PROFILE_INITIALS_PROPERTY);
		this.bio = profile.getString(PROFILE_BIO_PROPERTY);
		List<Value<String>> categoriesValueList = profile.getList(INSTITUTION_PROFILE_CATEGORIES_PROPERTY);
		List<String> categoriesStringList = categoriesValueList.stream().map(value->value.get()).collect(Collectors.toList());
		this.categories = new String[categoriesStringList.size()];
		categoriesStringList.toArray(this.categories);
		
	}
	
	
	public InstitutionProfile(String name, String initials, String bio, String[] categories) {
		this.name = name;
		this.initials = initials;
		this.bio = bio;
		this.categories = categories;
	}
	 
	public boolean badData() {
		if(name == null) 
			return true;
		if(initials == null) 
			return true;
		if(bio ==null) 
			return true;
		if(categories == null) 
			return true;
		return false;
	}
	 
}

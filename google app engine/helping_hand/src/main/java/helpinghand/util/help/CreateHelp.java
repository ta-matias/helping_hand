/**
 * 
 */
package helpinghand.util.help;

import static helpinghand.util.GeneralUtils.badString;

/**
 * @author PogChamp Software
 *
 */
public class CreateHelp {
	
	public String name;
	public String creator;
	public String description;
	public String time;
	public boolean permanent;
	public double[] location;
	public String[] conditions;
	
	public CreateHelp() {}
	
	public CreateHelp(String name, String creator,String description, String time,boolean permanent, double[] location, String[] conditions) {
		this.name = name;
		this.creator = creator;
		this.description = description;
		this.time = time;
		this.permanent = permanent;
		this.location = location;
		this.conditions = conditions;
	}
	
	public boolean badData() {
		if(badString(name))return true;
		if(badString(creator))return true;
		if(badString(description))return true;
		if(badString(time))return true;
		if(location == null || location.length != 2)return true;
		if(conditions == null) return true;
		return false;
	}

}

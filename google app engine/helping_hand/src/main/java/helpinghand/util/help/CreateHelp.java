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
	public double[] location;
	
	public CreateHelp() {}
	
	public CreateHelp(String name, String creator,String description, String time,double[] location) {
		this.name = name;
		this.creator = creator;
		this.description = description;
		this.time = time;
		this.location = location;
	}
	
	public boolean badData() {
		if(badString(name))
			return true;
		if(badString(creator))
			return true;
		if(badString(description))
			return true;
		if(badString(time))
			return true;
		if(location == null || location.length != 2)
			return true;
		return false;
	}

}

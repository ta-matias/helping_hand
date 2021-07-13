/**
 * 
 */
package helpinghand.util.help;

import static helpinghand.util.GeneralUtils.badString;

import com.google.cloud.Timestamp;
/**
 * @author PogChamp Software
 *
 */
public class CreateHelp {
	
	public String name;
	public String description;
	public String time;
	public double[] location;
	
	public CreateHelp() {}
	
	public CreateHelp(String name, String description, String time, double[] location) {
		this.name = name;
		this.description = description;
		this.time = time;
		this.location = location;
	}
	
	public boolean badData() {
		if(badString(name)) 
			return true;
		if(badString(description)) 
			return true;
		if(location == null || location.length != 2)
			return true;
		
		try {
			Timestamp now = Timestamp.now();
			Timestamp timeStamp = Timestamp.parseTimestamp(time);
			
			if(badString(time) || timeStamp.compareTo(now) < 0) 
				return true;
		}catch(Exception e) {
			return true;
		}
		
		return false;	
	}

}
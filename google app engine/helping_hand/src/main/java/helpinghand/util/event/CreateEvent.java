/**
 * 
 */
package helpinghand.util.event;

import static helpinghand.util.GeneralUtils.badString;

import com.google.cloud.Timestamp;
/**
 * @author PogChamp Software
 *
 */
public class CreateEvent {
	
	public String name;
	public String description;
	public String start;
	public String end;
	public double[] location;
	
	public CreateEvent() {}
	
	public CreateEvent(String name, String description, String start, String end, double[] location) {
		this.name = name;
		this.description = description;
		this.start = start;
		this.end = end;
		this.location = location;
	}
	
	/**
	 * Validates the attributes of the event.
	 * @return true, if the attributes are valid.
	 * 		   false, otherwise.
	 */
	public boolean badData() {
		if(badString(name)) return true;
		if(badString(description)) return true;
		if(location == null || location.length != 2)return true;

		try {
			Timestamp now = Timestamp.now();
			Timestamp startStamp = Timestamp.parseTimestamp(start);
			Timestamp endStamp = Timestamp.parseTimestamp(end);
			
			if(startStamp.compareTo(now) < 0) return true;
			if(endStamp.compareTo(startStamp) < 0)return true;
		} catch(Exception e) {
			return true;
		}
		
		return false;	
	}

}
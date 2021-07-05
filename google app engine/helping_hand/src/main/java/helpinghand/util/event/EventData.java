/**
 * 
 */
package helpinghand.util.event;

import static helpinghand.util.GeneralUtils.badString;

/**
 * @author PogChamp Software
 *
 */
public class EventData {

	public long id;
	public String name;
	public String creator;
	public String description;
	public String start;
	public String end;
	public double[] location;
	public String[] conditions;
	
	public EventData() {}
	
	public EventData(long id, String name, String creator, String description, String start, String end, double[] location, String[] conditions) {
		this.id = id;
		this.name = name;
		this.creator = creator;
		this.description = description;
		this.start = start;
		this.end = end;
		this.location = location;
		this.conditions = conditions;
	}
	
	/**
	 * Validates the attributes of the event.
	 * @return true, if the attributes are valid.
	 * 		   false, otherwise.
	 */
	public boolean badData() {
		if(badString(name)) 
			return true;
		if(badString(creator)) 
			return true;
		if(badString(description)) 
			return true;
		if(badString(start)) 
			return true;
		if(badString(end)) 
			return true;
		if(location == null || location.length != 2)
			return false;
		if(conditions == null) 
			return true;
		return false;
	}

}

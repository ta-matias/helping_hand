package helpinghand.util.event;

import static helpinghand.util.GeneralUtils.badString;
import static helpinghand.resources.EventResource.EVENT_NAME_PROPERTY;
import static helpinghand.resources.EventResource.EVENT_CREATOR_PROPERTY;
import static helpinghand.resources.EventResource.EVENT_DESCRIPTION_PROPERTY;
import static helpinghand.resources.EventResource.EVENT_START_PROPERTY;
import static helpinghand.resources.EventResource.EVENT_END_PROPERTY;
import static helpinghand.resources.EventResource.EVENT_LOCATION_PROPERTY;



import com.google.cloud.datastore.Entity;

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
	
	public EventData() {}
	
	public EventData(Entity event) {
		this.id = event.getKey().getId();
		this.name = event.getString(EVENT_NAME_PROPERTY);
		this.creator = event.getString(EVENT_CREATOR_PROPERTY);
		this.description = event.getString(EVENT_DESCRIPTION_PROPERTY);
		this.start = event.getTimestamp(EVENT_START_PROPERTY).toString();
		this.end = event.getTimestamp(EVENT_END_PROPERTY).toString();
		this.location = new double[] {event.getLatLng(EVENT_LOCATION_PROPERTY).getLatitude(),event.getLatLng(EVENT_LOCATION_PROPERTY).getLongitude()};
	}
	
	public EventData(long id, String name, String creator, String description, String start, String end, double[] location) {
		this.id = id;
		this.name = name;
		this.creator = creator;
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
		if(badString(creator)) return true;
		if(badString(description)) return true;
		if(badString(start)) return true;
		if(badString(end)) return true;
		if(location == null || location.length != 2)return false;
		return false;
	}

}
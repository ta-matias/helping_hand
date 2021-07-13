package helpinghand.util.help;

import com.google.cloud.datastore.Entity;

import static helpinghand.resources.HelpResource.HELP_NAME_PROPERTY;
import static helpinghand.resources.HelpResource.HELP_CREATOR_PROPERTY;
import static helpinghand.resources.HelpResource.HELP_DESCRIPTION_PROPERTY;
import static helpinghand.resources.HelpResource.HELP_TIME_PROPERTY;
import static helpinghand.resources.HelpResource.HELP_LOCATION_PROPERTY;

public class HelpData {
	
	public long id;
	public String name;
	public String creator;
	public String description;
	public String time;
	public double[] location;
	
	public HelpData() {}
	
	public HelpData(Entity help) {
		this.id = help.getKey().getId();
		this.name = help.getString(HELP_NAME_PROPERTY);
		this.creator = help.getString(HELP_CREATOR_PROPERTY);
		this.description = help.getString(HELP_DESCRIPTION_PROPERTY);
		this.time = help.getTimestamp(HELP_TIME_PROPERTY).toString();
		this.location = new double[] {help.getLatLng(HELP_LOCATION_PROPERTY).getLatitude(),help.getLatLng(HELP_LOCATION_PROPERTY).getLongitude()};
	}

	public HelpData(long id, String name, String creator, String description, String time, double[] location) {
		this.id = id;
		this.name = name;
		this.creator = creator;
		this.description = description;
		this.time = time;
		this.location = location;
	}
	
}

/**
 * 
 */
package helpinghand.util.event;

/**
 * @author PogChamp Software
 *
 */
public class EventDataPublic {
	
	public String name;
	public String beginDate;
	public String endDate;
	public String description;
	public String conditions;
	public String address;
	public String location;
	
	public EventDataPublic() {
		
	}
	
	public EventDataPublic(String name, String beginDate, String endDate, String description, String conditions, String address, String location) {
		this.name = name;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.description = description;
		this.conditions = conditions;
		this.address = address;
		this.location = location;
	}

}

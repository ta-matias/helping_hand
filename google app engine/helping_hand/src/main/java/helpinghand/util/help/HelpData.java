/**
 * 
 */
package helpinghand.util.help;

/**
 * @author PogChamp Software
 *
 */
public class HelpData {
	
	public String name;
	public String helpId;
	public String creatorId;
	public String helpDate;
	public String address;
	public String location;
	
	public HelpData() {
		
	}
	
	public HelpData(String name, String helpId, String creatorId, String helpDate, String address, String location) {
		this.name = name;
		this.helpId = helpId;
		this.creatorId = creatorId;
		this.helpDate = helpDate;
		this.address = address;
		this.location = location;
	}
	
	public boolean validate() {
		if(name == null)
			return false;
		if(helpId == null)
			return false;
		if(creatorId == null)
			return false;
		if(helpDate == null)
			return false;
		if(address == null)
			return false;
		if(location == null)
			return false;
		return true;
	}

}

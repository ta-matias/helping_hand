package helpinghand.util.help;

import static helpinghand.util.GeneralUtils.badString;

public class FinishHelp {
		
	public int rating;
	public String helper;
	
	public FinishHelp() {}
	
	public FinishHelp (int rating, String helper) {
		this.rating = rating;
		this.helper = helper;
	}
	
	public boolean badData() {
		if(rating > 5 || rating < 0)
			return true;
		if(badString(helper))
			return true;
		return false;
	}
	
}

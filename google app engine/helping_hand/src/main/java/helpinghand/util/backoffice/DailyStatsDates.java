package helpinghand.util.backoffice;

import static helpinghand.util.GeneralUtils.badString;

public class DailyStatsDates {

	public String startDate;
	public String endDate;
	
	public DailyStatsDates() {}
	
	public DailyStatsDates(String startDate, String endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public boolean badData() {
		if(badString(startDate))return true;
		if(badString(endDate))return true;
		return false;
	}
}

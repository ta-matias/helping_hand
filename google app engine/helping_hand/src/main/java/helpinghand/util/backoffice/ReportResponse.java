package helpinghand.util.backoffice;

import static helpinghand.util.GeneralUtils.badString;

public class ReportResponse {
	
	public String message;
	
	public ReportResponse() {}
	
	public ReportResponse(String message) {
		this.message = message;
	}
	
	public boolean badData() {
		if(badString(message))return true;
		return false;
	}
}

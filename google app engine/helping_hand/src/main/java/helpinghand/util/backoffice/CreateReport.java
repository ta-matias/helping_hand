package helpinghand.util.backoffice;

import static helpinghand.util.GeneralUtils.badString;



public class CreateReport {
	
	public String subject;
	public String text;
	
	public CreateReport() {}

	public CreateReport(String subject, String text) {
		this.subject = subject;
		this.text = text;
	}
	
	public boolean badData() {
		if(badString(subject))return true;
		if(badString(text))return true;
		return false;
	}
}

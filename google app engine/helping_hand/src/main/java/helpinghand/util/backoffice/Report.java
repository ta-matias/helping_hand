package helpinghand.util.backoffice;

import com.google.cloud.datastore.Entity;
import static helpinghand.resources.BackOfficeResource.REPORT_DATE_PROPERTY;
import static helpinghand.resources.BackOfficeResource.REPORT_CREATOR_PROPERTY;
import static helpinghand.resources.BackOfficeResource.REPORT_SUBJECT_PROPERTY;
import static helpinghand.resources.BackOfficeResource.REPORT_TEXT_PROPERTY;

public class Report {
	
	public long id;
	public String date;
	public String creator;
	public String subject;
	public String text;
	
	public Report() {}
	
	public Report(Entity report) {
		this.id = report.getKey().getId();
		this.date = report.getTimestamp(REPORT_DATE_PROPERTY).toString();
		this.creator = report.getString(REPORT_CREATOR_PROPERTY);
		this.subject = report.getString(REPORT_SUBJECT_PROPERTY);
		this.text = report.getString(REPORT_TEXT_PROPERTY);
	}
	
}

package helpinghand.util.help;

import static helpinghand.resources.UserResource.USER_STATS_RATING_PROPERTY;
import static helpinghand.resources.UserResource.USER_STATS_REQUESTS_DONE_PROPERTY;
import static helpinghand.resources.UserResource.USER_STATS_REQUESTS_PROMISED_PROPERTY;
import static helpinghand.util.account.AccountUtils.ACCOUNT_ID_PROPERTY;

import com.google.cloud.datastore.Entity;

import static helpinghand.util.account.AccountUtils.ACCOUNT_EMAIL_PROPERTY;

public class HelperStats {
	
	public String id;
	public String email;
	public double rating;
	public double reliability;
	
	public HelperStats() {}
	
	public HelperStats(Entity account, Entity stats) {
		this.id = account.getString(ACCOUNT_ID_PROPERTY);
		this.email = account.getString(ACCOUNT_EMAIL_PROPERTY);
		this.rating = stats.getDouble(USER_STATS_RATING_PROPERTY);

		double promised = Double.valueOf(stats.getLong(USER_STATS_REQUESTS_PROMISED_PROPERTY));
		double done = Double.valueOf(stats.getLong(USER_STATS_REQUESTS_DONE_PROPERTY));

		if(promised == 0.0)
			this.reliability = 0;
		else
			this.reliability = done/promised;
	}
	
}

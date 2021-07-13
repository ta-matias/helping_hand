package helpinghand.util.user;

import static helpinghand.resources.UserResource.USER_STATS_RATING_PROPERTY;
import static helpinghand.resources.UserResource.USER_STATS_REQUESTS_DONE_PROPERTY;
import static helpinghand.resources.UserResource.USER_STATS_REQUESTS_PROMISED_PROPERTY;

import com.google.cloud.datastore.Entity;

public class UserStats {
	
	public double rating;
	public double reliability;
	
	public UserStats() {}
	
	public UserStats(Entity stats) {
		this.rating = stats.getDouble(USER_STATS_RATING_PROPERTY);
		
		double promised = Double.valueOf(stats.getLong(USER_STATS_REQUESTS_PROMISED_PROPERTY));
		double done = Double.valueOf(stats.getLong(USER_STATS_REQUESTS_DONE_PROPERTY));

		if(promised == 0.0)
			this.reliability = 0;
		else
			this.reliability = done/promised;
	}
	
	public UserStats(double rating, double reliability) {
		this.rating = rating;
		this.reliability = reliability;
	}
	
}

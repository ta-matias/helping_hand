package helpinghand.util.user;

public class UserFeed {
	public String[] feed;
	
	public UserFeed() {}
	
	public UserFeed(String[] feed) {
		this.feed= feed;
	}
	
	public boolean badData() {
		if(feed == null)return true;
		return false;
	}
}

package helpinghand.util.account;

public class AccountFeed {
	
	public String[] feed;
	
	public AccountFeed() {}
	
	public AccountFeed(String[] feed) {
		this.feed= feed;
	}
	
	public boolean badData() {
		if(feed == null)
			return true;
		return false;
	}
}

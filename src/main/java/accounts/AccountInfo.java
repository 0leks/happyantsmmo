package accounts;

public class AccountInfo {
	
	public int id;
	public String googleid;
	public String handle;
	public int x;
	public int y;

	public AccountInfo(int id, String googleid, String username, int x, int y) {
		this.id = id;
		this.googleid = googleid;
		this.handle = username;
		this.x = x;
		this.y = y;
	}
}

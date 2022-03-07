package accounts;

import java.beans.JavaBean;

@JavaBean
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGoogleid() {
		return googleid;
	}

	public void setGoogleid(String googleid) {
		this.googleid = googleid;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
	
	
}

package ok.accounts;

import java.beans.JavaBean;

import org.json.JSONObject;

@JavaBean
public class AccountInfo {
	
	public int id;
	public String googleid;
	public String handle;

	public AccountInfo(int id, String googleid, String username) {
		this.id = id;
		this.googleid = googleid;
		this.handle = username;
	}

	public AccountInfo(String googleid) {
		this.googleid = googleid;
	}
	
	public boolean exists() {
		return handle != null;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}
	
	@Override
	public String toString() {
		return id + "," + googleid + "," + handle;
	}
	
	public JSONObject toJSONObject() {
		return new JSONObject().put("handle", handle);
	}
}


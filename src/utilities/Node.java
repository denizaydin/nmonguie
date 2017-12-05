package utilities;

import java.util.Hashtable;

public class Node {
private String ip;
private String id;
private Hashtable<String,String> attributes;

	public Node(String ip) {
		this.ip = ip;
		this.attributes = new Hashtable<String,String>();
	}
	
	public Node(String ip,String id) {
		this.ip = ip;
		this.id=id;
		this.attributes = new Hashtable<String,String>();

	}

	/**
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the attributes
	 */
	public Hashtable<String, String> getAttributes() {
		return attributes;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "node [ip=" + ip + ", id=" + id + ", attributes=" + attributes + "]";
	}

	
}

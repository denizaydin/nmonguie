package models;

import java.net.InetAddress;

public class Dslam {

	  private String name;
	  private int deviceid;
	  private InetAddress ip;
	  private int dbID;
	  private int popID;
	  private String popName;
	  private float latitude;
	  private float longitude;
	  private String Int;
	  private InetAddress PeIp;
	  private int PeDeviceId;
	  private int PedbId;
	  private String PeInt;
	  private int PePopId;
	  private String PePopName;

	public Dslam(String name,int deviceid,InetAddress ip, int popID,String popname) {
	  this.name = name;
	  this.deviceid = deviceid;
	  this.ip = ip;
	  this.popID = popID;
	  this.popName = popname;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the dbID
	 */
	public int getDbID() {
		return dbID;
	}

	/**
	 * @param dbID the dbID to set
	 */
	public void setDbID(int dbID) {
		this.dbID = dbID;
	}

	/**
	 * @return the popID
	 */
	public int getPopID() {
		return popID;
	}

	/**
	 * @param popID the popID to set
	 */
	public void setPopID(int popID) {
		this.popID = popID;
	}

	/**
	 * @return the popName
	 */
	public String getPopName() {
		return popName;
	}

	/**
	 * @param popName the popName to set
	 */
	public void setPopName(String popName) {
		this.popName = popName;
	}

	/**
	 * @return the latitude
	 */
	public float getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public float getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the peIp
	 */
	public InetAddress getPeIp() {
		return PeIp;
	}

	/**
	 * @param peIp the peIp to set
	 */
	public void setPeIp(InetAddress peIp) {
		PeIp = peIp;
	}

	/**
	 * @return the peDeviceId
	 */
	public int getPeDeviceId() {
		return PeDeviceId;
	}

	/**
	 * @param peDeviceId the peDeviceId to set
	 */
	public void setPeDeviceId(int peDeviceId) {
		PeDeviceId = peDeviceId;
	}

	/**
	 * @return the pedbId
	 */
	public int getPedbId() {
		return PedbId;
	}

	/**
	 * @param pedbId the pedbId to set
	 */
	public void setPedbId(int pedbId) {
		PedbId = pedbId;
	}

	/**
	 * @return the peInt
	 */
	public String getPeInt() {
		return PeInt;
	}

	/**
	 * @param peInt the peInt to set
	 */
	public void setPeInt(String peInt) {
		PeInt = peInt;
	}

	/**
	 * @return the deviceid
	 */
	public int getDeviceid() {
		return deviceid;
	}

	/**
	 * @return the ip
	 */
	public InetAddress getIp() {
		return ip;
	}

	/**
	 * @return the int
	 */
	public String getInt() {
		return Int;
	}

	/**
	 * @param i the int to set
	 */
	public void setInt(String i) {
		Int = i;
	}

	
	/**
	 * @return the pePopId
	 */
	public int getPePopId() {
		return PePopId;
	}

	/**
	 * @param pePopId the pePopId to set
	 */
	public void setPePopId(int pePopId) {
		PePopId = pePopId;
	}

	/**
	 * @return the pePopName
	 */
	public String getPePopName() {
		return PePopName;
	}

	/**
	 * @param pePopName the pePopName to set
	 */
	public void setPePopName(String pePopName) {
		PePopName = pePopName;
	}


	public String toString() {
		return "Dslam [" + (name != null ? "name=" + name + ", " : "") + "deviceid=" + deviceid + ", "
				+ (ip != null ? "ip=" + ip + ", " : "") + "dbID=" + dbID + ", popID=" + popID + ", "
				+ (popName != null ? "popName=" + popName + ", " : "") + "latitude=" + latitude + ", longitude="
				+ longitude + ", " + (Int != null ? "Int=" + Int + ", " : "")
				+ (PeIp != null ? "PeIp=" + PeIp + ", " : "") + "PeDeviceId=" + PeDeviceId + ", PedbId=" + PedbId + ", "
				+ (PeInt != null ? "PeInt=" + PeInt + ", " : "") + "PePopId=" + PePopId + ", "
				+ (PePopName != null ? "PePopName=" + PePopName : "") + "]";
	}




	 
	
	
	
}

package models;

import java.net.InetAddress;
import java.util.Hashtable;
import org.snmp4j.UserTarget;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import snmpinterface.SnmpTarget;
import snmpinterface.SnmpUsm;
import topologydiscovery.IgpOid;










public class IgpNode
{
  private InetAddress igpRouterID;
  private String igpID;
  private int igpType;
  private int igpL2toL1Leaking;
  private String igpArea;
  private OID igpBaseOid;
  public static final Hashtable<String, String> igpSubOids;
  static {
	  igpSubOids = new Hashtable<String, String>();
	  igpSubOids.put("ciiSysObjecOID", "1.1.1");
	  igpSubOids.put("ciiNextCircOID", "1.3");
	  igpSubOids.put("ciiCircLevelValues","1.4");
	  igpSubOids.put("ciiISAdjlisteners","1.6");
	}

  private Hashtable<String, IgpOid> igpOids;
  private Hashtable<String, IgpInterface> igpinterfaces;
  private Hashtable<String, String> ifIndexToigpCircuitIndex;

  private String hostname;
  private int popID;
  private String popName;
  private float latitude;
  private float longitude;
  private int deviceid;
  private int dbID;
  
  private InetAddress snmpipv4address;
  private String snmpVersion;
  private String snmpCommunity;
  private String snmpUser;
  private String snmpPass;
  private String snmpPriv;
  private OctetString snmpengineID;
  private UserTarget snmpTarget;
  private UsmUser snmpUsmUser;
  public boolean ciiSysObject;
  
  /**
   * An internal error status as node have an error 
   * 
  */
  private boolean internalErrorStatus;
  /**
   * An internal error message indication the error description
   * 
  */
  private String internalErrorMessage;
  private String updatestatus;
  private Long updatetime;
  private String status;
  private String statusmessage;
  
  public IgpNode(InetAddress igpRouterID)
  {
    this.igpRouterID = igpRouterID;
    igpType = -1;
    igpL2toL1Leaking = -1;
    this.igpinterfaces = new Hashtable<String, IgpInterface>(); 
    this.ifIndexToigpCircuitIndex = new Hashtable<String, String>();
    this.igpOids=new Hashtable<String, IgpOid>();
  }
  
  public IgpNode(String igpid)
  {
    igpID = igpid;
    igpType = -1;
    igpL2toL1Leaking = -1;
    this.igpinterfaces = new Hashtable<String, IgpInterface>(); 
    this.ifIndexToigpCircuitIndex = new Hashtable<String, String>();
    this.igpOids=new Hashtable<String, IgpOid>();
  }
  


  public String getIgpID()
  {
    return igpID;
  }
  


  public void setIgpID(String igpID)
  {
    this.igpID = igpID;
  }
  


  public int getIgpType()
  {
    return igpType;
  }
  


  public void setIgpType(int igpTYPE)
  {
    igpType = igpTYPE;
  }
  


  public int getIgpL2toL1Leaking()
  {
    return igpL2toL1Leaking;
  }
  


  public void setIgpL2toL1Leaking(int l2toL1Leaking)
  {
    igpL2toL1Leaking = l2toL1Leaking;
  }
  


  public String getIgpArea()
  {
    return igpArea;
  }
  


  public void setIgpArea(String igparea)
  {
    igpArea = igparea;
  }
  


  public String getHostname()
  {
    return hostname;
  }
  


  public void setHostname(String hostname)
  {
    this.hostname = hostname;
  }
  


  public InetAddress getIgpRouterID()
  {
    return igpRouterID;
  }
  


  public void setIgpRouterID(InetAddress routerid)
  {
    igpRouterID = routerid;
  }
  



  public Hashtable<String, IgpInterface> getIgpinterfaces()
  {
    return igpinterfaces;
  }
  


  public void setIgpinterfaces(Hashtable<String, IgpInterface> igpinterfaces)
  {
    this.igpinterfaces = igpinterfaces;
  }
  


  public Hashtable<String, String> getIfIndexToigpCircuitIndex()
  {
    return ifIndexToigpCircuitIndex;
  }
  


  public String getSnmpUser()
  {
    return snmpUser;
  }
  


  public String getSnmpVersion()
  {
    return snmpVersion;
  }
  


  public void setSnmpVersion(String snmpVersion)
  {
    this.snmpVersion = snmpVersion;
  }
  


  public String getSnmpCommunity()
  {
    return snmpCommunity;
  }
  


  public void setSnmpCommunity(String snmpCommunity)
  {
    this.snmpCommunity = snmpCommunity;
  }
  


  public void setSnmpUser(String snmpUser)
  {
    this.snmpUser = snmpUser;
  }
  


  public String getSnmpPass()
  {
    return snmpPass;
  }
  


  public void setSnmpPass(String snmpPass)
  {
    this.snmpPass = snmpPass;
  }
  


  public String getSnmpPriv()
  {
    return snmpPriv;
  }
  


  public void setSnmpPriv(String snmpPriv)
  {
    this.snmpPriv = snmpPriv;
  }
  



  public InetAddress getSnmpipv4address()
  {
    return snmpipv4address;
  }
  


  public void setSnmpipv4address(InetAddress snmpipv4address)
  {
    this.snmpipv4address = snmpipv4address;
  }
  



  public UserTarget getSnmpTarget()
  {
    snmpTarget = new SnmpTarget(getSnmpUser(), getSnmpipv4address()).getUsertarget();
    return snmpTarget;
  }
  

  public UsmUser getSnmpUsmUser()
  {
    snmpUsmUser = new SnmpUsm(getSnmpUser(), getSnmpPass(), getSnmpPriv()).getUsmuser();
    return snmpUsmUser;
  }
  





  public OctetString getSnmpengineID()
  {
    return snmpengineID;
  }
  


  public void setSnmpengineID(OctetString snmpengineID)
  {
    this.snmpengineID = snmpengineID;
  }

  public OID getIgpBaseOid()
  {
    return igpBaseOid;
  }
  


  public void setIgpBaseOid(OID igpbaseoid)
  {
    igpBaseOid = igpbaseoid;
  }
  


  public int getPopID()
  {
    return popID;
  }
  


  public void setPopID(int popID)
  {
    this.popID = popID;
  }
  


  public String getPopName()
  {
    return popName;
  }
  


  public void setPopName(String popName)
  {
    this.popName = popName;
  }
  


  public float getLatitude()
  {
    return latitude;
  }
  


  public void setLatitude(float latitude)
  {
    this.latitude = latitude;
  }
  


  public float getLongitude()
  {
    return longitude;
  }
  


  public void setLongitude(float longitude)
  {
    this.longitude = longitude;
  }
  


  public int getDeviceid()
  {
    return deviceid;
  }
  


  public void setDeviceid(int dbdeviceid)
  {
    deviceid = dbdeviceid;
  }
  


  public int getDbID()
  {
    return dbID;
  }
  


  public void setDbID(int dbID)
  {
    this.dbID = dbID;
  }
  
  public boolean getInternalErrorStatus() {
		return internalErrorStatus;
	}

	public void setInternalErrorStatus(boolean intErrorStatus) {
		this.internalErrorStatus = intErrorStatus;
	}


  /**
 * @return the errorMessage
 */
public String getInternalErrorMessage() {
	return internalErrorMessage;
}

/**
 * @param errorMessage the errorMessage to set
 */
public void setInternalErrorMessage(String errorMessage) {
	internalErrorMessage = errorMessage;
}


/**
 * @return the status
 */
public String getStatus() {
	return status;
}

/**
 * @param status the status to set
 */
public void setStatus(String status) {
	this.status = status;
}

/**
 * @return the statusmessage
 */
public String getStatusmessage() {
	return statusmessage;
}

/**
 * @param statusmessage the statusmessage to set
 */
public void setStatusmessage(String statusmessage) {
	this.statusmessage = statusmessage;
}



/**
 * @return the igpOids
 */
public Hashtable<String, IgpOid> getIgpOids() {
	return igpOids;
}

/**
 * @return the updatestatus
 */
public String getUpdatestatus() {
	return updatestatus;
}

/**
 * @param updatestatus the updatestatus to set
 */
public void setUpdatestatus(String updatestatus) {
	this.updatestatus = updatestatus;
}

/**
 * @return the updatetime
 */
public Long getUpdatetime() {
	return updatetime;
}

/**
 * @param updatetime the updatetime to set
 */
public void setUpdatetime(Long updatetime) {
	this.updatetime = updatetime;
}

public String toString() {
	return "IgpNode [igpRouterID=" + igpRouterID + ", igpID=" + igpID + ", igpType=" + igpType + ", igpL2toL1Leaking="
			+ igpL2toL1Leaking + ", igpArea=" + igpArea + ", igpBaseOid=" + igpBaseOid + ", igpinterfaces="
			+ igpinterfaces + ", ifIndexToigpCircuitIndex=" + ifIndexToigpCircuitIndex + ", hostname=" + hostname
			+ ", popID=" + popID + ", popName=" + popName + ", latitude=" + latitude + ", longitude=" + longitude
			+ ", deviceid=" + deviceid + ", dbID=" + dbID + ", snmpipv4address=" + snmpipv4address + ", snmpVersion="
			+ snmpVersion + ", snmpCommunity=" + snmpCommunity + ", snmpUser=" + snmpUser + ", snmpPass=" + snmpPass
			+ ", snmpPriv=" + snmpPriv + ", snmpengineID=" + snmpengineID + ", snmpTarget=" + snmpTarget
			+ ", snmpUsmUser=" + snmpUsmUser + ", internalErrorStatus=" + internalErrorStatus + ", internalErrorMessage=" + internalErrorMessage
			+ "]";
}



}
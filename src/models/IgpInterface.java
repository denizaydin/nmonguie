package models;

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class IgpInterface
{
  private String ifIndex;
  private String ifDesc;
  private String ifLastChange;
  private String ifAlias;
  private int ifAdminStatus;
  private int ifOperStatus;
  private String ifNet;
  private String intDBID;
  private String mainintDBID;
  private String circuitDBID;
  private String igpNeighborID;
  private String igpCircuitIndex;
  private String igpInterfaceType;
  private String igpNeighborLevel;
  private String igpLevel;
  private String igpMetric;
  private boolean isPassive;
  
  
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
  
  private static final Logger logger = LoggerFactory.getLogger("main");
  
  public IgpInterface(String igpCircuitIndex) {
    this.igpCircuitIndex = igpCircuitIndex;
    logger.debug("Created new igpinterface with igpCircuitIndex:" + this.igpCircuitIndex);
  }
  
  public IgpInterface(String ifIndex,String intDBID,String ifDesc) {
	  this.ifIndex = ifIndex;
	  this.intDBID = intDBID;
	  this.ifDesc = ifDesc;
	  logger.debug("Created new igpinterface with ifIndex:" + this.ifIndex);

}




  public String getIfIndex()
  {
    return ifIndex;
  }
  

  public String getIfDesc()
  {
    return ifDesc;
  }
  


  public void setIfIndex(String ifIndex)
  {
    this.ifIndex = ifIndex;
  }
  


  public void setIfDesc(String ifDesc)
  {
    this.ifDesc = ifDesc;
  }
  


  public String getIfLastChange()
  {
    return ifLastChange;
  }
  


  public void setIfLastChange(String ifLastChange)
  {
    this.ifLastChange = ifLastChange;
  }
  


  public String getIfAlias()
  {
    return ifAlias;
  }
  


  public void setIfAlias(String ifAlias)
  {
    this.ifAlias = ifAlias;
  }
  


  public int getIfAdminStatus()
  {
    return ifAdminStatus;
  }
  


  public void setIfAdminStatus(int ifAdminStatus)
  {
    this.ifAdminStatus = ifAdminStatus;
  }
  


  public int getIfOperStatus()
  {
    return ifOperStatus;
  }
  


  public void setIfOperStatus(int ifOperStatus)
  {
    this.ifOperStatus = ifOperStatus;
  }
  


  public String getIfNet()
  {
    return this.ifNet;
  }
  














  public void setIfNet(String ip)
  {
    String[] iparray = ip.split("\\:");
	ip = Integer.parseInt(iparray[0], 16)+"."+Integer.parseInt(iparray[1], 16)+"."+Integer.parseInt(iparray[2], 16)+"."+Integer.parseInt(iparray[3], 16);
	SubnetUtils subnetUtils = new SubnetUtils(ip+"/30");
    this.ifNet=subnetUtils.getInfo().getNetworkAddress();
  }
  




  public String getIgpInterfaceType()
  {
    return igpInterfaceType;
  }
  


  public boolean isPassive()
  {
    return isPassive;
  }
  


  public void setPassive(boolean isPassive)
  {
    this.isPassive = isPassive;
  }
  


  public void setIgpInterfaceType(String igpInterfaceType)
  {
    if (!igpInterfaceType.equals("2")) logger.warn("unsupported igp interface type:" + igpInterfaceType);
    this.igpInterfaceType = igpInterfaceType;
  }
  


  public String getIgpNeighborID()
  {
    return igpNeighborID;
  }
  


  public void setIgpNeighborID(String igpNeighborID)
  {
    this.igpNeighborID = igpNeighborID;
  }
  


  public String getIgpNeighborLevel()
  {
    return igpNeighborLevel;
  }
  


  public void setIgpNeighborLevel(String igpNeighborLevel)
  {
    this.igpNeighborLevel = igpNeighborLevel;
  }
  


  public String getIgpLevel()
  {
    return igpLevel;
  }
  


  public void setIgpLevel(String igpLevel)
  {
    this.igpLevel = igpLevel;
  }
  


  public String getIgpMetric()
  {
    return igpMetric;
  }
  


  public void setIgpMetric(String igpMetric)
  {
    this.igpMetric = igpMetric;
  }
  


  public String getIgpCircuitIndex()
  {
    return igpCircuitIndex;
  }
  


  public String getIntDBID()
  {
    return intDBID;
  }
  


  public void setIntDBID(String intDBID)
  {
    this.intDBID = intDBID;
  }
  


  public String getMainintDBID()
  {
    return mainintDBID;
  }
  


  public void setMainintDBID(String mainintDBID)
  {
    this.mainintDBID = mainintDBID;
  }
  


  public int getStatus()
  {
    int status = -1;
    if (ifOperStatus != 1) {
      if (ifAdminStatus != 1) {
        status = 16;
      } else {
        status = 32;
      }
    } else {
      status = 0;
    }
    return status;
  }
  


  public String getCircuitDBID()
  {
    return circuitDBID;
  }
  


  public void setCircuitDBID(String circuitDBID)
  {
    this.circuitDBID = circuitDBID;
  }





/**
 * @return the internalErrorStatus
 */
public boolean isInternalErrorStatus() {
	return internalErrorStatus;
}





/**
 * @param internalErrorStatus the internalErrorStatus to set
 */
public void setInternalErrorStatus(boolean internalErrorStatus) {
	this.internalErrorStatus = internalErrorStatus;
}





/**
 * @return the internalErrorMessage
 */
public String getInternalErrorMessage() {
	return internalErrorMessage;
}





/**
 * @param internalErrorMessage the internalErrorMessage to set
 */
public void setInternalErrorMessage(String internalErrorMessage) {
	this.internalErrorMessage = internalErrorMessage;
}





/* (non-Javadoc)
 * @see java.lang.Object#toString()
 */
@Override
public String toString() {
	return "IgpInterface [ifIndex=" + ifIndex + ", ifDesc=" + ifDesc + ", ifLastChange=" + ifLastChange + ", ifAlias="
			+ ifAlias + ", ifAdminStatus=" + ifAdminStatus + ", ifOperStatus=" + ifOperStatus + ", ifNet=" + ifNet
			+ ", intDBID=" + intDBID + ", mainintDBID=" + mainintDBID + ", circuitDBID=" + circuitDBID
			+ ", igpNeighborID=" + igpNeighborID + ", igpCircuitIndex=" + igpCircuitIndex + ", igpInterfaceType="
			+ igpInterfaceType + ", igpNeighborLevel=" + igpNeighborLevel + ", igpLevel=" + igpLevel + ", igpMetric="
			+ igpMetric + ", isPassive=" + isPassive + ", internalErrorStatus=" + internalErrorStatus
			+ ", internalErrorMessage=" + internalErrorMessage + "]";
}

  
  
}
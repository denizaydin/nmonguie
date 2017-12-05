package snmpinterface;

import org.snmp4j.security.UsmUser;

public class SnmpUsm
{
  private UsmUser usmuser;
  
  public SnmpUsm(String username, String password, String privpassword)
  {
    usmuser = new UsmUser(
      new org.snmp4j.smi.OctetString(username), 
      org.snmp4j.security.AuthMD5.ID, new org.snmp4j.smi.OctetString(password), 
      org.snmp4j.security.PrivDES.ID, new org.snmp4j.smi.OctetString(privpassword));
  }
  

  public UsmUser getUsmuser()
  {
    return usmuser;
  }
}
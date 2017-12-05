package snmpinterface;

import java.net.Inet4Address;
import java.net.InetAddress;
import org.snmp4j.UserTarget;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;



public class SnmpTarget
{
  private UserTarget usertarget;
  
  public SnmpTarget(String username, InetAddress ipv4address)
  {
    usertarget = new UserTarget();
    usertarget.setVersion(3);
    usertarget.setAddress(new UdpAddress(((Inet4Address)ipv4address).getHostAddress() + "/161"));
    usertarget.setSecurityLevel(3);
    usertarget.setSecurityName(new OctetString(username));
    usertarget.setTimeout(5000L);
  }
  


  public UserTarget getUsertarget()
  {
    return usertarget;
  }
}
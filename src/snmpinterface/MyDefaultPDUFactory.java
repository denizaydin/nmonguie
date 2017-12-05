package snmpinterface;

import org.snmp4j.smi.OctetString;

public class MyDefaultPDUFactory extends org.snmp4j.util.DefaultPDUFactory
{

	private OctetString contextEngineId;
  
  public MyDefaultPDUFactory(int pduType, OctetString contextEngineId) {
    super(pduType);
	this.contextEngineId = contextEngineId;

  }

public OctetString getContextEngineId() {
	return contextEngineId;
} 
}
package topologydiscovery;

import org.snmp4j.smi.OID;

/**
 * @author denizaydin
 *
 */
public class IgpOid  {
/**
* Name of the IgpOid
*/
public String name;
/**
* Oid
*/
private OID oid;
/**
* status indicating OID collection is completed?
*/
public boolean status;
/**
* descriptiong of the oid
*/
public String desc;

public IgpOid (String name,OID oid) {
	this.name = name;
	this.oid = oid;
	this.status=false;	
}


/**
 * @return the name of the OID
 */
public String getName() {
	return name;
}


/**
 * @return the oid
 */
public OID getOid() {
	return oid;
}



}
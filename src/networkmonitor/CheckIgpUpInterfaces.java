/**
 *
 */
package networkmonitor;

import dbinterface.InventoryDatabase;
import dbinterface.Neo4jDatabase;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import models.IgpInterface;
import models.IgpNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import snmpinterface.SetInterfaceAttributes;



public class CheckIgpUpInterfaces implements Runnable
{
	private Thread thread;
	private String threadName;
	private Boolean stop = Boolean.valueOf(false);

	private InventoryDatabase inventoryDatabase;
    private Neo4jDatabase neo4jDatabase;
	private Hashtable<String, IgpNode> IgpNodes;


	private static final Logger logger = LoggerFactory.getLogger("topologydiscovery");

	public CheckIgpUpInterfaces(String threadname, InventoryDatabase inventoryDatabase, Neo4jDatabase neo4jDatabase) {
		threadName = threadname;
		this.neo4jDatabase = neo4jDatabase;
		this.inventoryDatabase = inventoryDatabase;
		this.IgpNodes = new Hashtable<String, IgpNode>();
		logger.debug("created up interface checker Thread, threadname " + threadName);
	}


	public void run()
	{
		logger.info("running IgpInterface status checker with threadname " + threadName);
		try {
			while (!stop) {
				long waitTime=300000;

				logger.info("checing up interfaces. if any of them is down will set its status to current status(16/32)");
				this.IgpNodes = neo4jDatabase.getIgpInterfacesWithStatus(0);
				if (this.IgpNodes != null) {
					logger.info(this.IgpNodes.size() + " number of igpnodes found from igpinformer");
					Iterator<String> iterators = this.IgpNodes.keySet().iterator();
					while (iterators.hasNext()) {
						String key = (String)iterators.next();
						if (inventoryDatabase.getNode(this.IgpNodes.get(key))) {
							logger.info("found igpnode " + (this.IgpNodes.get(key)).getHostname() + " routerid" + (this.IgpNodes.get(key)).getIgpRouterID() + " in the database");
						} else {
							logger.error("can not found igpnode " + (this.IgpNodes.get(key)).getIgpRouterID() + " ,removing from the list");
							iterators.remove();
						}
					} 
					logger.info(this.IgpNodes.size() + " number of igpnodes found in the database");
					CollectIgpInterfaceAttributes(this.IgpNodes);
					
					Hashtable<String, IgpInterface> downIgpInterfaces = new Hashtable<String, IgpInterface>();

					for (String igpnode : this.IgpNodes.keySet()) {
						for (String igpint : this.IgpNodes.get(igpnode).getIgpinterfaces().keySet()) {
							downIgpInterfaces.put(igpnode,this.IgpNodes.get(igpnode).getIgpinterfaces().get(igpint));
						}
					}
					neo4jDatabase.setIgpInterfacesStatus(downIgpInterfaces,"igpUPinterfaceChecker");
				} else {
					logger.error("null igp node list from GetIGPNodesFromIGPInformer");
					waitTime=120000;
				}
				
				Thread.sleep(waitTime);				

			}
		} catch (InterruptedException e) {
			logger.info("Thread " + threadName + " interrupted." + e.toString());
		}
		logger.info("Thread " + threadName + " exiting.");

	}

	public void start()
	{
		logger.debug("Starting " + threadName);
		if (thread == null) {
			thread = new Thread(this, threadName);
			thread.start();
		}
	}

	private Hashtable<String, IgpNode> CollectIgpInterfaceAttributes(Hashtable<String, IgpNode> IgpNodes) 
	{
			Snmp snmp;
			OctetString contextEngineId = new OctetString("0002651100[02]");
			try {
			snmp = new Snmp(new DefaultUdpTransportMapping());
			USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(usm);
			logger.debug("snmp interface created with engine ID " + contextEngineId.toString());
			snmp.listen();
			} catch (IOException e) {
				//FIX:We can not return IGP nodes at this stage we may have null values for DB to update!
				logger.error("can not create snmp instance " + e.toString());
				return(this.IgpNodes);
			}
			logger.info("collecting information for "+ IgpNodes.size() +"number of nodes");
			Iterator<String> iterators = IgpNodes.keySet().iterator();
			while (iterators.hasNext()) {
				String key = (String)iterators.next();
				if ( (this.IgpNodes.get(key)).getSnmpengineID() != null ) {
					snmp.getUSM().addUser(new OctetString(this.IgpNodes.get(key).getSnmpUser()), this.IgpNodes.get(key).getSnmpengineID(),this.IgpNodes.get(key).getSnmpUsmUser());
					logger.debug("node "+ (this.IgpNodes.get(key)).getIgpRouterID() + " already have snmp engineID");
					continue;
				}
				logger.debug("tring to set snmp engineid of igp node " + (this.IgpNodes.get(key)).getIgpRouterID() + " " + (this.IgpNodes.get(key)).getSnmpUser() + " " + (this.IgpNodes.get(key)).getSnmpPass());
				byte[] engineIdBytes = snmp.discoverAuthoritativeEngineID((this.IgpNodes.get(key)).getSnmpTarget().getAddress(), 1000);
				if (engineIdBytes != null) {
					OctetString engineId = new OctetString(engineIdBytes);
					logger.debug("discovered engineId for host:" + (this.IgpNodes.get(key)).getIgpRouterID() + " " + engineId);
					this.IgpNodes.get(key).setSnmpengineID(engineId);
					snmp.getUSM().addUser(new OctetString(this.IgpNodes.get(key).getSnmpUser()), this.IgpNodes.get(key).getSnmpengineID(),this.IgpNodes.get(key).getSnmpUsmUser());
				} else {
					logger.error("can not set snmp engineid" + (this.IgpNodes.get(key)).getIgpRouterID() + " in the database");
					iterators.remove();
					logger.warn("removed the node from the list");
				}
			}            
            logger.info("starting to collect interface attributes");
            Vector<SetInterfaceAttributes> InterfaceAttributesListeners = new Vector<SetInterfaceAttributes>(2, 10);
 		    iterators = this.IgpNodes.keySet().iterator();
			while (iterators.hasNext()) {
				String key = (String)iterators.next();
				if (this.IgpNodes.get(key).getIgpinterfaces() == null) {
					logger.error("null interface list for node "+ this.IgpNodes.get(key).getIgpRouterID() );
					continue;
				}
                logger.debug("collecting inteface attributes for node:" + this.IgpNodes.get(key).getIgpRouterID()+ " interface number " + this.IgpNodes.get(key).getIgpinterfaces().size());
                int interfaceCount = 0;
                ScopedPDU sysDescrPDU = new ScopedPDU();
                sysDescrPDU.setType(-96);
                Iterator<String> iterators2 = this.IgpNodes.get(key).getIgpinterfaces().keySet().iterator();
  
    			while (iterators2.hasNext()) {
    			String igpint = (String)iterators2.next();
    			//TODO
                if(this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex() == null ) {
                	logger.error("null ifIndex " + this.IgpNodes.get(key).getIgpRouterID()+ " interface number " );
                	continue;
                }
            		logger.debug("inteface " + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc() + " ifIndex " + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex());

                    String ifAdminStatusoid = "1.3.6.1.2.1.2.2.1.7." + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex();
                    String ifOperStatusoid = "1.3.6.1.2.1.2.2.1.8." + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex();
                    sysDescrPDU.add(new VariableBinding(new OID(ifAdminStatusoid)));
                    sysDescrPDU.add(new VariableBinding(new OID(ifOperStatusoid)));
                               
                if (++interfaceCount != 5) continue;
                logger.debug("reached max interface count" + interfaceCount);
                logger.debug("creating snmp listener for node " + this.IgpNodes.get(key).getSnmpTarget().getAddress() + " interface count " + interfaceCount);
                SetInterfaceAttributes InterfaceAttributesListener = new SetInterfaceAttributes(this.IgpNodes.get(key));
                InterfaceAttributesListeners.add(InterfaceAttributesListener);
                snmp.discoverAuthoritativeEngineID(this.IgpNodes.get(key).getSnmpTarget().getAddress(), 1000);
                try {
					snmp.send(sysDescrPDU, this.IgpNodes.get(key).getSnmpTarget(), null, InterfaceAttributesListener);
				} catch (IOException e) {
					logger.error("can not send snmp to this node " + this.IgpNodes.get(key).getIgpRouterID());
					this.IgpNodes.get(key).setInternalErrorStatus(true);
					this.IgpNodes.get(key).setInternalErrorMessage("snmp error");	
					continue;
				}
                logger.debug("sent snmp get to node " + this.IgpNodes.get(key).getSnmpTarget().getAddress());
                sysDescrPDU = new ScopedPDU();
                sysDescrPDU.setType(-96);
                interfaceCount = 0;
                }
    			if (interfaceCount <= 0) continue;
                logger.debug("creating snmp listener for node" + this.IgpNodes.get(key).getSnmpTarget().getAddress() + " interface count " + interfaceCount);
                SetInterfaceAttributes InterfaceAttributesListener = new SetInterfaceAttributes(this.IgpNodes.get(key));
                InterfaceAttributesListeners.add(InterfaceAttributesListener);
                snmp.discoverAuthoritativeEngineID(this.IgpNodes.get(key).getSnmpTarget().getAddress(), 1000);
                try {
					snmp.send(sysDescrPDU, this.IgpNodes.get(key).getSnmpTarget(), null, InterfaceAttributesListener);
				} catch (IOException e) {
					logger.error("can not send snmp to this node " + this.IgpNodes.get(key).getIgpRouterID());
					this.IgpNodes.get(key).setInternalErrorStatus(true);
					this.IgpNodes.get(key).setInternalErrorMessage("snmp error");	
					continue;
				}
                logger.debug("sent snmp get to node " + this.IgpNodes.get(key).getSnmpTarget().getAddress());
			}
            boolean finished = false;
            while (!finished) {
                boolean responselistenersFinished = true;
                for (SetInterfaceAttributes responselistener : InterfaceAttributesListeners) {
                    logger.debug("is responselistenersFinished?:" + responselistenersFinished);
                    logger.debug("checking responselistener for node " + responselistener.getIgpnode().getSnmpTarget().getAddress() + " runtime:" + responselistener.getRunTime());
                    if (!responselistener.isFinished() && responselistener.getRunTime() > (responselistener.getIgpnode().getSnmpTarget().getTimeout() + 5000) / 1000) {
                        logger.error("long runtime:" + responselistener.getRunTime() + " for responselistener of node " + responselistener.getIgpnode().getSnmpTarget().getAddress());
                        responselistener.stopWalk();
                    }
            		if (responselistenersFinished == true && responselistener.isFinished()) {
            			responselistenersFinished = true;
					} else {
						responselistenersFinished = responselistener.isFinished();
					}
                }
                    if (responselistenersFinished) {
                        logger.debug("all snmp listners are finished, ending check");
                        finished = responselistenersFinished;
                        continue;
                    } else {
                    logger.debug("all snmp listners are not FINISHED, will retry check");
                    try {
                        logger.debug("sleeping for 5 seconds");
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) {
	        			logger.error("can not wait " + e.toString());
                    }
                    }
                
            }
            
            logger.info("information collection ended, checking interface attributes");
            
            
            
			try {
				snmp.close();
			} catch (IOException e) {
				logger.error("can not close snmp " + e.toString());
			}
		return IgpNodes;
	}

	public Thread getThread()
	{
		return thread;
	}





}
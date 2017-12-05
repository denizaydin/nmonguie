/**
 *
 */
package topologydiscovery;

import dbinterface.InventoryDatabase;
import dbinterface.Neo4jDatabase;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import models.IgpNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
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
import org.snmp4j.util.TreeUtils;
import snmpinterface.MyDefaultPDUFactory;
import snmpinterface.SetIgpBaseOid;
import snmpinterface.SetInterfaceAttributesWithIgpCircuitIndex;
import snmpinterface.SetModel;




public class UpdateIgpNodes implements Runnable
{
	private Thread thread;
	private String threadName;
	private Boolean stop = false;

	private InventoryDatabase inventoryDatabase;
    private Neo4jDatabase neo4jDatabase;
	private Hashtable<String, IgpNode> IgpNodes;
 
	private MainIgpDiscovery igpdiscovery;

	private static final Logger logger = LoggerFactory.getLogger("igpupdate");

	public UpdateIgpNodes(String threadname, InventoryDatabase inventoryDatabase, Neo4jDatabase neo4jDatabase, MainIgpDiscovery igpdiscovery) {
		threadName = threadname;
		this.neo4jDatabase = neo4jDatabase;
		this.inventoryDatabase = inventoryDatabase;
		this.IgpNodes = new Hashtable<String, IgpNode>();
		this.igpdiscovery = igpdiscovery;
		logger.debug("created igp update Thread, threadname " + threadName);
	}


	public void run()
	{
		logger.info("running igp update with threadname " + threadName);
		try {
			while (!stop) {
				while(this.igpdiscovery.isRunning) {
					logger.warn("topologydiscovery is running, waiting");
					Thread.sleep(5000);
				}
				long waitTime=600000;
				try {
				this.IgpNodes = this.neo4jDatabase.getNodesToUpdate();
				if (this.IgpNodes != null) {
				Iterator<String> iterators = this.IgpNodes.keySet().iterator();
				while (iterators.hasNext()) {
					String key = (String)iterators.next();
					if (inventoryDatabase.getNode(this.IgpNodes.get(key))) {
						logger.info("found igpnode " + (this.IgpNodes.get(key)).getHostname() + " routerid" + (this.IgpNodes.get(key)).getIgpRouterID() + " in the database");
					} else {
						logger.error("can not found igpnode " + (this.IgpNodes.get(key)).getIgpRouterID() + " in the database, removing from the list");
						iterators.remove();
					}
				}
				
				logger.info(this.IgpNodes.size() + " number of igpnodes found in the database");
				neo4jDatabase.addOrUpdateNodes(CollectInformationFromIGPNodes(IgpNodes),"igpnodeupdater");
				
				
				} else {
					logger.info("noting to update");
					waitTime=65000;
				}
				} catch (Exception e) {
					logger.error("error while tring to get igp nodes from the graph database " + e.toString());
					waitTime=65000;
				}				
				logger.info("waiting for "+ waitTime + " miliseconds");				
			Thread.sleep(waitTime);
			}
		} catch (InterruptedException e) {
			logger.info("Thread " + threadName + " interrupted.");
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


	private Hashtable<String, IgpNode> CollectInformationFromIGPNodes(Hashtable<String, IgpNode> IgpNodes) 
	{
		Snmp snmp;
		OctetString contextEngineId = new OctetString("0002651100[02]");
		try {
			snmp = new Snmp(new DefaultUdpTransportMapping());
			USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(usm);
			snmp.listen();
		} catch (IOException e) {
			//FIX:We can not return IGP nodes at this stage we may have null values for DB to update!
			logger.error("can not create snmp instance " + e.toString());
			return(this.IgpNodes);
		}
		logger.info("collecting information from nodes");
		Iterator<String> iterators = IgpNodes.keySet().iterator();
		while (iterators.hasNext()) {
			String key = (String)iterators.next();
			if ( (this.IgpNodes.get(key)).getSnmpengineID() != null ) {
				snmp.getUSM().addUser(new OctetString(this.IgpNodes.get(key).getSnmpUser()), this.IgpNodes.get(key).getSnmpengineID(),this.IgpNodes.get(key).getSnmpUsmUser());
				logger.info("node "+ (this.IgpNodes.get(key)).getIgpRouterID() + " already have snmp engineID");
				continue;
			}
			logger.debug("tring to set snmp engineid of igp node " + (this.IgpNodes.get(key)).getIgpRouterID() + " " + (this.IgpNodes.get(key)).getSnmpUser() + " " + (this.IgpNodes.get(key)).getSnmpPass());
			byte[] engineIdBytes = snmp.discoverAuthoritativeEngineID((this.IgpNodes.get(key)).getSnmpTarget().getAddress(), 3000);
			if (engineIdBytes != null) {
				OctetString engineId = new OctetString(engineIdBytes);
				logger.info("discovered engineId for host:" + (this.IgpNodes.get(key)).getIgpRouterID() + " " + engineId);
				this.IgpNodes.get(key).setSnmpengineID(engineId);
				snmp.getUSM().addUser(new OctetString(this.IgpNodes.get(key).getSnmpUser()), this.IgpNodes.get(key).getSnmpengineID(),this.IgpNodes.get(key).getSnmpUsmUser());
			} else {
				logger.error("can not set snmp engineid for igp node" + (this.IgpNodes.get(key)).getIgpRouterID() + ", removing from the list");
				iterators.remove();
			}
		}

		logger.info("tring to find node's igp oid for "+ IgpNodes.size()+ " number of nodes");
		Vector<SetIgpBaseOid> responselisteners = new Vector<SetIgpBaseOid>(2, 10);
		iterators = IgpNodes.keySet().iterator();
		while (iterators.hasNext()) {
			String key = (String)iterators.next();
			if (this.IgpNodes.get(key).getIgpBaseOid() != null) {
				logger.info(this.IgpNodes.get(key).getIgpRouterID() + " have igp base oid, passing");
				continue;
			}
			ScopedPDU sysDescrPDU = new ScopedPDU();
			sysDescrPDU.setType(PDU.GET);
			sysDescrPDU.add(new VariableBinding(new OID("1.3.6.1.2.1.1.1.0")));
			logger.debug("creating snmp listener for node " + this.IgpNodes.get(key).getSnmpTarget().getAddress());
			SetIgpBaseOid responselistener = new SetIgpBaseOid(this.IgpNodes.get(key));
			responselisteners.add(responselistener);
			try {
				snmp.discoverAuthoritativeEngineID(this.IgpNodes.get(key).getSnmpTarget().getAddress(), 1000);
				snmp.send(sysDescrPDU, this.IgpNodes.get(key).getSnmpTarget(), null, responselistener);
			} catch (IOException e) {
				logger.error("can not send snmp to this node " + this.IgpNodes.get(key).getIgpRouterID());
				this.IgpNodes.get(key).setInternalErrorStatus(true);
				this.IgpNodes.get(key).setInternalErrorMessage("snmp error");	
				continue;
			}
			logger.debug("sent snmp get request for sysDescr oid 1.3.6.1.2.1.1.1.0 to node " + this.IgpNodes.get(key).getSnmpTarget().getAddress());
		}
		boolean finished = false;
		while (!finished) {
			boolean responselistenersFinished = true;
			for (SetIgpBaseOid responselistener : responselisteners) {
				logger.debug("is responselistenersFinished?:" + responselistenersFinished);
				logger.debug("checking responselistener for target:" + responselistener.getIgpnode().getSnmpTarget().getAddress() + " runtime:" + responselistener.getRunTime());
				if (!responselistener.isFinished() && responselistener.getRunTime() > (responselistener.getIgpnode().getSnmpTarget().getTimeout() + 5000) / 1000) {
					logger.error("long runtime:" + responselistener.getRunTime() + " for responselistener of host:" + responselistener.getIgpnode().getSnmpTarget().getAddress());
					responselistener.getIgpnode().setInternalErrorStatus(true);
					responselistener.getIgpnode().setInternalErrorMessage("snmp timeout");
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
					logger.error("unable to sleep:" + e.getMessage());
				}
			}

		}
		iterators = IgpNodes.keySet().iterator();
		while (iterators.hasNext()) {
			String key = (String)iterators.next();
			if ( (this.IgpNodes.get(key)).getIgpBaseOid() == null ) {
				iterators.remove();
				logger.error("null igp base oid, removed node " + key + " from node list");
			} else {
				logger.info("igp base oid for node " + key + " " + this.IgpNodes.get(key).getIgpBaseOid().toString());
			}
		}


		for(String subigpOidName : IgpNode.igpSubOids.keySet()) {
			logger.debug("tring for sub igpOid " + subigpOidName + " " + IgpNode.igpSubOids.get(subigpOidName));
			//Create initial treelistener vector
			Vector<SetModel> treelisteners = new Vector<SetModel>(2, 10);

			iterators = IgpNodes.keySet().iterator();
			while (iterators.hasNext()) {
				String key = (String)iterators.next();
				if (this.IgpNodes.get(key).getIgpOids().get(subigpOidName) != null) {
					if(this.IgpNodes.get(key).getIgpOids().get(subigpOidName).status) {
					logger.info(subigpOidName + " is comleted for this node " + this.IgpNodes.get(key).getIgpRouterID());
					continue;
					}
				}
				IgpOid igpoid = new IgpOid(subigpOidName, new OID(this.IgpNodes.get(key).getIgpBaseOid().toString()+"."+IgpNode.igpSubOids.get(subigpOidName)));
				logger.debug("creating snmp listener for node " + this.IgpNodes.get(key).getSnmpTarget().getAddress() + " and for " + igpoid.getName());
				SetModel treelistener = new SetModel(this.IgpNodes.get(key),igpoid);
				treelisteners.add(treelistener);
				TreeUtils treeUtils = new TreeUtils(snmp, new MyDefaultPDUFactory(PDU.GETNEXT, contextEngineId));
				treeUtils.setMaxRepetitions(100);
				this.IgpNodes.get(key).getIgpOids().put(subigpOidName, igpoid);
				//snmp.discoverAuthoritativeEngineID(this.IgpNodes.get(key).getSnmpTarget().getAddress(), 1000);
				treeUtils.getSubtree(this.IgpNodes.get(key).getSnmpTarget(), igpoid.getOid(), null, treelistener);
				logger.debug("sent snmp request for oid " + igpoid.getOid().toString() + this.IgpNodes.get(key).getSnmpTarget().getAddress());
			}
			finished = false;
			while (!finished) {
				boolean treelistenersFinished = true;
				for (SetModel treelistener : treelisteners) {
					logger.debug("are all the treelisteners finished?:" + treelistenersFinished);
					if (!treelistener.isFinished() && treelistener.getRunTime() > (treelistener.getIgpnode().getSnmpTarget().getTimeout() + 5000) / 1000) {
						logger.error("long runtime:" + treelistener.getRunTime() + " for treelistener of host:" + treelistener.getIgpnode().getSnmpTarget().getAddress() + " and oid " + treelistener.getIgpOid().getName());
						treelistener.getIgpnode().setInternalErrorStatus(true);
						treelistener.getIgpnode().setInternalErrorMessage("snmp timeout");
						treelistener.getIgpOid().status=false;
						treelistener.stopWalk();
					}
					if (treelistenersFinished == true && treelistener.isFinished()) {
						treelistenersFinished = true;
					} else {
						treelistenersFinished = treelistener.isFinished();
					}                
				}
				if (treelistenersFinished) {
					logger.debug("all snmp listners are finished, ending check");
					finished = treelistenersFinished;
					continue;
				} else {
					logger.debug("all snmp listners are not FINISHED, wating for 5 seconds");
					try {
						Thread.sleep(5000);
					}
					catch (InterruptedException e) {
						logger.error("can not wait " + e.toString());

					}
				}
			}


		}



		// check for mandatory interface attributes            
		logger.info("starting to collect interface attributes");
		Vector<SetInterfaceAttributesWithIgpCircuitIndex> InterfaceAttributesListeners = new Vector<SetInterfaceAttributesWithIgpCircuitIndex>(2, 10);
		iterators = this.IgpNodes.keySet().iterator();
		while (iterators.hasNext()) {
			String key = (String)iterators.next();
			if (this.IgpNodes.get(key).getIgpinterfaces() == null) {
				logger.error("null interface list for node "+ this.IgpNodes.get(key).getIgpRouterID() );
				this.IgpNodes.get(key).setInternalErrorStatus(true);
				this.IgpNodes.get(key).setInternalErrorMessage("no interface defined");
				continue;
			}
			logger.debug("collect inteface attributes for node:" + this.IgpNodes.get(key).getIgpRouterID()+ " interface number " + this.IgpNodes.get(key).getIgpinterfaces().size());
			int interfaceCount = 0;
			ScopedPDU sysDescrPDU = new ScopedPDU();
			sysDescrPDU.setType(-96);
			Iterator<String> iterators2 = this.IgpNodes.get(key).getIgpinterfaces().keySet().iterator();

			while (iterators2.hasNext()) {
				String igpint = (String)iterators2.next();
				if(this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex() == null ) {
					logger.error("null ifIndex " + this.IgpNodes.get(key).getIgpRouterID()+ " interface number " );
					this.IgpNodes.get(key).setInternalErrorStatus(true);
					this.IgpNodes.get(key).setInternalErrorMessage("no interface defined");
					continue;
				}
				String ifDescoid = "1.3.6.1.2.1.2.2.1.2." + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex();
				String ifAliasoid = "1.3.6.1.2.1.31.1.1.1.18." + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex();
				String ifAdminStatusoid = "1.3.6.1.2.1.2.2.1.7." + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex();
				String ifOperStatusoid = "1.3.6.1.2.1.2.2.1.8." + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex();
				sysDescrPDU.add(new VariableBinding(new OID(ifDescoid)));
				sysDescrPDU.add(new VariableBinding(new OID(ifAliasoid)));
				sysDescrPDU.add(new VariableBinding(new OID(ifAdminStatusoid)));
				sysDescrPDU.add(new VariableBinding(new OID(ifOperStatusoid)));

				if (++interfaceCount != 5) continue;
				logger.debug("reached max interface count" + interfaceCount);
				logger.debug("creating snmp listener for node " + this.IgpNodes.get(key).getSnmpTarget().getAddress() + " interface count " + interfaceCount);
				SetInterfaceAttributesWithIgpCircuitIndex InterfaceAttributesListener = new SetInterfaceAttributesWithIgpCircuitIndex(this.IgpNodes.get(key));
				InterfaceAttributesListeners.add(InterfaceAttributesListener);
				//snmp.discoverAuthoritativeEngineID(this.IgpNodes.get(key).getSnmpTarget().getAddress(), 1000);
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
			SetInterfaceAttributesWithIgpCircuitIndex InterfaceAttributesListener = new SetInterfaceAttributesWithIgpCircuitIndex(this.IgpNodes.get(key));
			InterfaceAttributesListeners.add(InterfaceAttributesListener);
			//snmp.discoverAuthoritativeEngineID(this.IgpNodes.get(key).getSnmpTarget().getAddress(), 1000);
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
		finished = false;
		while (!finished) {
			boolean responselistenersFinished = true;
			for (SetInterfaceAttributesWithIgpCircuitIndex responselistener : InterfaceAttributesListeners) {
				logger.debug("is responselistenersFinished?:" + responselistenersFinished);
				logger.debug("checking responselistener for node " + responselistener.getIgpnode().getSnmpTarget().getAddress() + " runtime:" + responselistener.getRunTime());
				if (!responselistener.isFinished() && responselistener.getRunTime() > (responselistener.getIgpnode().getSnmpTarget().getTimeout() + 5000) / 1000) {
					logger.error("long runtime:" + responselistener.getRunTime() + " for responselistener of node " + responselistener.getIgpnode().getSnmpTarget().getAddress());
					responselistener.getIgpnode().setInternalErrorStatus(true);
					responselistener.getIgpnode().setInternalErrorMessage("snmp timeout");
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

		iterators = this.IgpNodes.keySet().iterator();
		while (iterators.hasNext()) {
			String key = (String)iterators.next();
			if (this.IgpNodes.get(key).getInternalErrorStatus()==true) continue;

			if (this.IgpNodes.get(key).getIgpinterfaces() == null) {
				logger.error("null interface list for node "+ this.IgpNodes.get(key).getIgpRouterID() );
				this.IgpNodes.get(key).setInternalErrorStatus(true);
				this.IgpNodes.get(key).setInternalErrorMessage("no interface defined");
				continue;
			}

			Iterator<String> iterators2 = this.IgpNodes.get(key).getIgpinterfaces().keySet().iterator();
			while (iterators2.hasNext()) {
				String igpint = (String)iterators2.next();
				if (this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfNet() == null) {
					if (this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc()!=null ) {
						if (this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().equals("Loopback0") ) {
							iterators2.remove();
						} else {
							logger.error("null ifNet for node " + this.IgpNodes.get(key).getIgpRouterID() + " interface CircuitIndex " + igpint + " " + this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc());
						}
					} else {
						logger.warn("null ifNet for node " + this.IgpNodes.get(key).getIgpRouterID() + " interface CircuitIndex " + igpint );
					}
				} else if (this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc()==null) {
					logger.error("null ifDesc for node " + this.IgpNodes.get(key).getIgpRouterID() + " interface CircuitIndex " + igpint );
					iterators2.remove();	                        
				} else if (this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc()!=null ) {
					if (this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().equals("Loopback0") ) {
						iterators2.remove();
					}
				} else if (this.IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex()==null) {
					logger.error("null ifIndex for node " + this.IgpNodes.get(key).getIgpRouterID() + " interface CircuitIndex " + igpint );
					iterators2.remove();	                       
				}

			}

		}

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
/**
 *
 */
package topologydiscovery;

import dbinterface.InventoryDatabase;
import dbinterface.Neo4jDatabase;

import models.IgpNode;

import snmpinterface.MyDefaultPDUFactory;
import snmpinterface.SetIgpBaseOid;
import snmpinterface.SetInterfaceAttributesWithIgpCircuitIndex;
import snmpinterface.SetModel;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;





public class MainIgpDiscovery implements Runnable
{
	private Thread thread;
	private String threadName;
	private Boolean stop = Boolean.valueOf(false);

	private InventoryDatabase inventoryDatabase;
	private Neo4jDatabase neo4jDatabase;
	private IgpNode igpinformer;
	private Hashtable<String, IgpNode> IgpNodes;
	public boolean isRunning;

	private static final Logger logger = LoggerFactory.getLogger("topologydiscovery");

	public MainIgpDiscovery(String threadname, InetAddress igpinformerigpRouterId, InventoryDatabase inventoryDatabase, Neo4jDatabase neo4jDatabase) {
		threadName = threadname;
		this.neo4jDatabase = neo4jDatabase;
		this.inventoryDatabase = inventoryDatabase;
		this.igpinformer = new IgpNode(igpinformerigpRouterId);
		this.IgpNodes = new Hashtable<String, IgpNode>();
		logger.debug("created IgpDiscovery Thread, threadname " + threadName);
	}


	public void run()
	{
		logger.info("running IgpDiscovery with threadname " + threadName);

		try {
			while (!stop) {
				long waitTime=86400000;
				this.isRunning = true;
				//get nodes from igp informer
				try {
					this.IgpNodes = GetIGPNodesFromIGPInformer(this.igpinformer);
					if (this.IgpNodes != null) {
						logger.info(this.IgpNodes.size() + " number of igpnodes found from igpinformer");
						
						// Now get nodes which areoutdated update time or which have updatestatus to update!
						Hashtable<String, IgpNode> IgpNodesToUpdate = new Hashtable<String, IgpNode>();
						Timestamp timestamp = new Timestamp(System.currentTimeMillis());
						IgpNodesToUpdate =	neo4jDatabase.getIgpNodes();
						if (IgpNodesToUpdate!= null) {
							Iterator<String> iterators =this.IgpNodes.keySet().iterator();
							while (iterators.hasNext()) {
								String key = (String)iterators.next();							
								if (IgpNodesToUpdate.get(key) != null ) {
									if (IgpNodesToUpdate.get(key).getUpdatestatus().equals("error")) {
									logger.info("found error node " + key + " updatetime " + new Date(IgpNodesToUpdate.get(key).getUpdatetime()).toString() );
									continue;
									} else if (IgpNodesToUpdate.get(key).getUpdatetime()< (timestamp.getTime()-86400000)) {
								    logger.info("found outdated node " + key + " updatetime " + new Date(IgpNodesToUpdate.get(key).getUpdatetime()).toString());
									} else {
									logger.info("found up to date node " + " status " + IgpNodesToUpdate.get(key).getUpdatestatus() + " "+ key + " updatetime " + new Date(IgpNodesToUpdate.get(key).getUpdatetime()).toString());
									iterators.remove();
									}
								}
							}
						} else {
							logger.info("no node returned from the graph database");
						}
						if (this.IgpNodes.size() != 0 ) {
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
							logger.info(this.IgpNodes.size() + " number of igpnodes found in the inventory database");
						} else {
							logger.info("no nodes found in the database");
						}
						
						if (this.IgpNodes.size() != 0 ) {
						neo4jDatabase.addOrUpdateNodes(CollectInformationFromIGPNodes(this.IgpNodes),"topologydiscovery");
						waitTime=65000;
						} else {
							logger.info("all nodes are up to date");
						}
						
				


					} else {
						logger.error("null igp node list from GetIGPNodesFromIGPInformer");
						waitTime=65000;
					}
				} catch (IOException e) {
					logger.error("can not get igp nodes from igpinformer " + e.toString());
					waitTime=5000;
				} catch (IgpDiscoveryExceptions e) {
					logger.error(e.toString());
					waitTime=5000;
				}
				logger.info("waiting for "+ waitTime + " miliseconds");
				this.isRunning = false;
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




	public Thread getThread()
	{
		return thread;
	}

	private Hashtable<String, IgpNode> GetIGPNodesFromIGPInformer(IgpNode igpInformer) throws IOException, IgpDiscoveryExceptions {
		logger.info("getting igpnodes from igpinformer via snmp");
		Hashtable<String, IgpNode> CollectedIgpNodes = new Hashtable<String, IgpNode>();
		try {
			Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
			USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(usm);
			snmp.listen();
			OctetString contextEngineId = new OctetString("0002651100[02]");			
			if(inventoryDatabase.getNode(igpInformer)) {				
				logger.info("found igp node in the inventory database");
				snmp.getUSM().addUser(new OctetString(igpInformer.getSnmpUser()), igpInformer.getSnmpUsmUser());
				// Some cisco routers do not support isismIB
				String isisMIB = "1.3.6.1.2.1.138.1.1.1.3.0";
				String ciscoIetfIsisMIB = "1.3.6.1.4.1.9.10.118.1.1.1.3.0";

				ScopedPDU pdu = new ScopedPDU();
				pdu.setType(-96);
				pdu.add(new VariableBinding(new OID(isisMIB)));
				pdu.add(new VariableBinding(new OID(ciscoIetfIsisMIB)));

				ResponseEvent responseEvent = snmp.send(pdu, igpInformer.getSnmpTarget());
				PDU response = responseEvent.getResponse();
				if ((response != null) && 
						(response.getErrorStatus() == 0)) {
					Vector<? extends VariableBinding> vbs = response.getVariableBindings();
					for (VariableBinding vb : vbs)
					{
						if (vb.getVariable().getClass().getName().equals("org.snmp4j.smi.OctetString")) {
							logger.debug("return oid " + vb.getOid() + " variable " + vb.getVariable().toString());
							vb.getOid().trim(5);
							igpInformer.setIgpBaseOid(vb.getOid());
						}
					}
				}


				if (igpInformer.getIgpBaseOid() == null) {
					snmp.close();
					throw new IgpDiscoveryExceptions(-1,"unknown igp base oid returned from igp informer OR the snmp variables are not correct!");

				}
			} else {
				throw new IgpDiscoveryExceptions(-1,"can not find igpinformer in the database");
			}

			logger.info("igpinformer igp base oid to "+igpInformer.getIgpBaseOid().toString());

			OID ciiRouterID = igpInformer.getIgpBaseOid().append(new OID("1.1.7.1.4"));
			TreeUtils treeUtils = new TreeUtils(snmp, new MyDefaultPDUFactory(PDU.GETNEXT, contextEngineId));
			treeUtils.setMaxRepetitions(100);
			List<TreeEvent> events = treeUtils.getSubtree(igpInformer.getSnmpTarget(), ciiRouterID);
			logger.debug("walking trough mib:" + ciiRouterID.toString() + " for igp informer");

			if (events == null || events.size() == 0) {
				snmp.close();
				throw new IgpDiscoveryExceptions(-1,"can not find any igp nodes in the igpinformer");
			}

			for (TreeEvent event : events) {
				if (event != null)
				{


					if (event.isError()) {
						logger.error("oid [" + ciiRouterID.toString() + "] " + event.getErrorMessage());
						return null;
					}
					else {
						VariableBinding[] varBindings = event.getVariableBindings();
						if ((varBindings == null) || (varBindings.length == 0)) {
							logger.warn("no variable returned for oid " + ciiRouterID.toString());
						}
						else {
							for (VariableBinding varBinding : varBindings)
								if (varBinding != null)
								{

									int igplevel = varBinding.getOid().last();
									varBinding.getOid().removeLast();
									String[] igpidarray = varBinding.getOid().toString().split("\\.");
									String igpid = Integer.toHexString(Integer.parseInt(igpidarray[14])) + Integer.toHexString(Integer.parseInt(igpidarray[15])) + 
											Integer.toHexString(Integer.parseInt(igpidarray[16])) + Integer.toHexString(Integer.parseInt(igpidarray[17])) + 
											Integer.toHexString(Integer.parseInt(igpidarray[18])) + Integer.toHexString(Integer.parseInt(igpidarray[19])).replaceAll("(^[0-9]$)", "0$1");
									logger.debug("formatted igpid:" + igpid);
									long routerid = Long.parseLong(varBinding.toValueString());
									byte[] bytes = new byte[4];
									bytes[0] = ((byte)(int)(routerid >> 24 & 0xFF));
									bytes[1] = ((byte)(int)(routerid >> 16 & 0xFF));
									bytes[2] = ((byte)(int)(routerid >> 8 & 0xFF));
									bytes[3] = ((byte)(int)(routerid & 0xFF));


									if (!(igpid.equals("193192126061")) ) {
										//continue;
									}
									if ((igpid.equals("193192126253")) ) {
										continue;
									}
									if ((igpid.equals("193192126224")) ) {
										continue;
									}
									if (CollectedIgpNodes.get(igpid) == null) {
										if (varBinding.toValueString().equals("0"))
										{
											igpInformer.setIgpType(igplevel);
											igpInformer.setIgpID(igpid);
											CollectedIgpNodes.put(igpid, igpInformer);
											logger.debug("added igpinformer to the igpnode list, igpid:" + igpInformer.getIgpID() + " igp type:" + igpInformer.getIgpType() + " routerid:" + igpInformer.getIgpRouterID());
										} else {
											IgpNode igpnode = new IgpNode(igpid);
											igpnode.setIgpType(igplevel);
											CollectedIgpNodes.put(igpid, igpnode);
											(CollectedIgpNodes.get(igpid)).setIgpRouterID((Inet4Address)Inet4Address.getByName((bytes[0] & 0xFF) + "." + (bytes[1] & 0xFF) + "." + (bytes[2] & 0xFF) + "." + (bytes[3] & 0xFF)));
											logger.debug("created new igp node and added to the igpnode list, igpid:" + igpnode.getIgpID() + " igp type:" + igpnode.getIgpType() + " routerid:" + igpnode.getIgpRouterID());
										}
									} else {
										logger.debug("node "+ CollectedIgpNodes.get(igpid).getIgpRouterID() + " already in the node list");
									}
								}
						}
					}
				}
			}
			logger.debug("information collect ended, returning result");
			snmp.close();
			return CollectedIgpNodes;
		}
		catch (IOException e)
		{

			return CollectedIgpNodes;
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
}
/**
 * 
 */
package dbinterface;


/**
 * @author denizaydin
 *
 */
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.Dslam;
import models.IgpInterface;
import models.IgpNode;

public class Neo4jDatabase {
	private String dbusername;
	private String dbpassword;
	private String connectionString;
	private InetAddress dbipv4address;
	private int dbport;
	private static final Logger logger = LoggerFactory.getLogger("database");

	public void configure(String dbusername, String dbpassword, InetAddress dbipv4address, int dbport) {
		this.dbusername = dbusername;
		this.dbpassword = dbpassword;
		this.dbipv4address = dbipv4address;
		this.dbport = dbport;
		this.connectionString = "bolt:/" + this.dbipv4address.toString() + ":" + this.dbport;
	}

	public boolean test() {
		Driver driver = GraphDatabase.driver(this.connectionString, AuthTokens.basic(this.dbusername, this.dbpassword));
		try (Session session = driver.session() ) {
			logger.info("connected to GraphDB!");
			driver.close();
			return true;
		} catch (Exception driverException) {
			logger.error("cannot connect to Graph DB! " + driverException.getMessage());
			driver.close();
			return false;
		}
	}


	public void addOrUpdateNodes(Hashtable<String, IgpNode> IgpNodes,String updater) {
		try (Driver driver = GraphDatabase.driver(this.connectionString, AuthTokens.basic(this.dbusername, this.dbpassword))) {
			try (Session session = driver.session() ) {
				logger.info("connected to GraphDB!");
				for (String key : IgpNodes.keySet()) {
					if (IgpNodes.get(key).getIgpRouterID() == null) {
						logger.error("null igpRouter ID for node "+ key);
						continue;
					}
					try (Transaction tx = session.beginTransaction()) {
						Timestamp timestamp = new Timestamp(System.currentTimeMillis());
						String currentdate;
						Date dt = new Date();
						SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						currentdate = sdf2.format(dt);
						String updatestatus = "updated";
						if (IgpNodes.get(key).getInternalErrorStatus()) {
							updatestatus = "error";
						}
						StatementResult result = tx.run("MERGE (router:router {igpid:{igpid}}) "
								+ "ON CREATE SET router.updater={updater},router.updatestatus={updatestatus},router.updatetime={timestamp},router.igpbaseoid={igpbaseoid},router.model='router',router.ip={ip},router.topology = '3.2',router.name={name},router.deviceid={deviceid},router.popid={popid},router.dblatitude=$latitude,router.dblongitude={longitude},router.popname={popname},router.status={status},router.message={message},router.statusupdater={updater},router.statusupdatetime={currentdate},router.igplevel = {igplevel},router.igpid={igpid},router.igpleaking={igpleaking} "
								+ "ON MATCH SET router.updater={updater},router.updatetime={timestamp},router.updatestatus={updatestatus},router.igpbaseoid={igpbaseoid},router.model='router',router.ip={ip},router.topology = '3.2',router.name={name},router.deviceid={deviceid},router.popid={popid},router.dblatitude=$latitude,router.dblongitude={longitude},router.popname={popname},router.igplevel = {igplevel},router.igpid={igpid},router.igpleaking={igpleaking} "
								+ "RETURN ID(router) as routerID",  
								Values.parameters("timestamp", timestamp.getTime(),
										"updater",updater,
										"currentdate", currentdate,
										"name", IgpNodes.get(key).getHostname(), 
										"ip", IgpNodes.get(key).getIgpRouterID().getHostAddress(), 
										"deviceid", IgpNodes.get(key).getDeviceid(), 
										"popid", IgpNodes.get(key).getPopID(), 
										"latitude", IgpNodes.get(key).getLatitude(), 
										"longitude", IgpNodes.get(key).getLongitude(), 
										"popname", IgpNodes.get(key).getPopName(), 
										"igplevel", IgpNodes.get(key).getIgpType(), 
										"igpid", IgpNodes.get(key).getIgpID(), 
										"igpleaking", IgpNodes.get(key).getIgpL2toL1Leaking(),
										"updatestatus",updatestatus,
										"status", 0, 
										"message", "created or updated by nmon via "+ updater,
										"igpbaseoid",IgpNodes.get(key).getIgpBaseOid().toString()));
						tx.success();
						while (result.hasNext()) {
							Record record = result.next();
							IgpNodes.get(key).setDbID(record.get("routerID").asInt());
							logger.debug("router is created with DBID:" + IgpNodes.get(key).getDbID() + " for node:" + IgpNodes.get(key).getIgpRouterID().getHostAddress());
						}
						String query = result.consume().statement().toString();
						logger.debug("router query is:" + (String)query);
						int theOnesCreated = result.consume().counters().nodesCreated();
						if (theOnesCreated == 1) {
							logger.debug("router is created with DBID:" + IgpNodes.get(key).getDbID() + " for node:" + IgpNodes.get(key).getIgpRouterID().getHostAddress());
						} else {
							logger.debug("node with DBID:" + IgpNodes.get(key).getDbID() + " updated" + " for node:" + IgpNodes.get(key).getIgpRouterID().getHostAddress());
						}
						tx.close();

						if (IgpNodes.get(key).getIgpinterfaces() == null) {
							logger.warn("empty interface list for node " + IgpNodes.get(key).getIgpRouterID());
							continue;
						}
						for (String igpint : IgpNodes.get(key).getIgpinterfaces().keySet()) {
							if (IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc() == null) {
								logger.error("null ifDesc for int " + igpint + " of node "+ key);
								continue;
							} else if (IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().equals("Loopback0")) {
								logger.warn("Skipping Loopback0 " + igpint + " of node "+ key);
								continue;								
							}
							if (IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfNet() == null) {
								logger.warn("null ifNet for int " + igpint + IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc() + " of node "+ key);	        	    			
								logger.debug("tring to add interface "+ IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc() + " for node " + key );
								if (IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().contains(".")) {
									//this is subinterface we should at this with main interface
									String mainintDesc = IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().substring(0, IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().indexOf("."));
									logger.debug("Should addorUpdate main interface:" + mainintDesc + " for this subint:" + IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc());

									try (Transaction intTx = session.beginTransaction() ) {	                          
										StatementResult intQueryResult = intTx.run("MATCH (router:router { ip:{ip}}) "
												+ "MERGE (mainint:interface {nodeip:{ip},name:{mainintname}} ) "
												+ "ON CREATE SET mainint.message={message},mainint.status={status},mainint.statusupdater = {updater},mainint.statusupdatetime = {currentdate},mainint.topology='2' "
												+ "MERGE (subint:interface {nodeip:{ip},name:{subintname},model:'logical interface'} ) "
												+ "ON CREATE SET subint.message={message},subint.status={status},subint.statusupdater={updater},subint.statusupdatetime={currentdate},subint.topology='2',subint.ifindex= {ifIndex},subint.description = {ifAlias} "
												+ "ON MATCH SET subint.ifindex= {ifIndex},subint.description = {ifAlias} "
												+ "MERGE (router)-[rm:CONTAIN]->(mainint) "
												+ "ON CREATE SET rm.topology='2' "
												+ "MERGE (mainint)-[ms:CONTAIN]->(subint) "
												+ "ON CREATE SET ms.topology='2' "
												+ "RETURN ID(mainint) as mainintID,ID(subint) as subintID", 
												Values.parameters("status", IgpNodes.get(key).getIgpinterfaces().get(igpint).getStatus(),
														"updater",updater,
														"currentdate", currentdate, 
														"ip", IgpNodes.get(key).getIgpRouterID().getHostAddress(), 
														"mainintname", mainintDesc, 
														"subintname", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc(), 
														"ifAlias", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfAlias(), 
														"ifIndex", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex(), 
														"popid", IgpNodes.get(key).getPopID(), 
														"popname", IgpNodes.get(key).getPopName(), 
														"status",  IgpNodes.get(key).getIgpinterfaces().get(igpint).getStatus(), 
														"message", "created by nmon via igp discovery"));
										intTx.success();
										while (intQueryResult.hasNext()) {
											Record record = intQueryResult.next();
											IgpNodes.get(key).getIgpinterfaces().get(igpint).setMainintDBID(Integer.toString(record.get("mainintID").asInt()));
											IgpNodes.get(key).getIgpinterfaces().get(igpint).setIntDBID(Integer.toString(record.get("subintID").asInt()));
											logger.debug("Maininterface DBID:" + IgpNodes.get(key).getIgpinterfaces().get(igpint).getMainintDBID() + " intDBID:" + IgpNodes.get(key).getIgpinterfaces().get(igpint).getIntDBID());
										}
										String intQuery = intQueryResult.consume().statement().toString();
										logger.debug("SubInterface creation Query is:" + intQuery);
										theOnesCreated = intQueryResult.consume().counters().nodesCreated();
										if (theOnesCreated == 1) {
											logger.debug("There were " + theOnesCreated + " the ones created.");
										} else {
											logger.debug("Node updated");
										}
										intTx.close();
									} catch (Exception transactionException) {
										logger.error("transaction error, while subinterface node adding" + transactionException.toString());
									}
								} else {
									try (Transaction intTx = session.beginTransaction() ) {
										StatementResult intQueryResult = intTx.run("MATCH (router:router { ip:{ip}}) "
												+ "MERGE (mainint:interface {nodeip:{ip},name:{mainintname}} ) "
												+ "ON CREATE SET mainint.type = 'mpls', mainint.statusupdater = {updater}, mainint.status={status}, mainint.message={message},mainint.alarmlevel = '0',mainint.statusupdatetime = {currentdate},mainint.topology='2',mainint.ifindex= {ifIndex},mainint.description = {ifAlias} "
												+ "MERGE (router)-[rm:CONTAIN]->(mainint) "
												+ "ON CREATE SET rm.status={status},rm.alarmlevel = '0',rm.statusupdater = {updater}, rm.statusupdatetime = {currentdate}, rm.topology='2' "
												+ "RETURN ID(mainint) as mainintID", 
												Values.parameters("status", IgpNodes.get(key).getIgpinterfaces().get(igpint).getStatus(),
														"updater",updater,
														"currentdate", currentdate, 
														"ip", IgpNodes.get(key).getIgpRouterID().getHostAddress(), 
														"mainintname", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc(), 
														"ifAlias", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfAlias(), 
														"ifIndex", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex(),
														"popid", IgpNodes.get(key).getPopID(), 
														"popname",IgpNodes.get(key).getPopName(), 
														"status",  IgpNodes.get(key).getIgpinterfaces().get(igpint).getStatus(), 
														"message", "created by nmon via igp discovery"));
										intTx.success();          
										while (intQueryResult.hasNext()) {
											Record record = intQueryResult.next();
											IgpNodes.get(key).getIgpinterfaces().get(igpint).setIntDBID(Integer.toString(record.get("mainintID").asInt()));
											logger.debug("intDBID:" + IgpNodes.get(key).getIgpinterfaces().get(igpint).getIntDBID());
										}
										String intQuery = intQueryResult.consume().statement().toString();
										logger.debug("SubInterface creation Query is:" + intQuery);
										theOnesCreated = intQueryResult.consume().counters().nodesCreated();
										if (theOnesCreated == 1) {
											logger.debug("There were " + theOnesCreated + " the ones created.");
										} else {
											logger.debug("Node updated");
										}
										intTx.close();
									} catch (Exception transactionException) {
										logger.error("transaction error, while maininterface node adding" + transactionException.toString());
									}
								}


								continue;
							}
							logger.debug("tring to add interface "+ IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc() + " for node " + key );
							if (IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().contains(".")) {
								//this is subinterface we should at this with main interface
								String mainintDesc = IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().substring(0, IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc().indexOf("."));
								logger.debug("Should addorUpdate main interface:" + mainintDesc + " for this subint:" + IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc());

								try (Transaction intTx = session.beginTransaction() ) {	                          
									StatementResult intQueryResult = intTx.run("MATCH (router:router { ip:{ip}}) "
											+ "MERGE (mainint:interface {nodeip:{ip},name:{mainintname}} ) "
											+ "ON CREATE SET mainint.message={message},mainint.status={status},mainint.statusupdater = {updater},mainint.statusupdatetime = {currentdate},mainint.topology='2' "
											+ "MERGE (subint:interface {nodeip:{ip},name:{subintname},model:'logical interface'} ) "
											+ "ON CREATE SET subint.message={message},subint.status={status},subint.statusupdater={updater},subint.statusupdatetime={currentdate},subint.topology='2',subint.ifindex= {ifIndex},subint.description = {ifAlias} "
											+ "ON MATCH SET subint.ifindex= {ifIndex},subint.description = {ifAlias} "
											+ "MERGE (router)-[rm:CONTAIN]->(mainint) "
											+ "ON CREATE SET rm.topology='2' "
											+ "MERGE (mainint)-[ms:CONTAIN]->(subint) "
											+ "ON CREATE SET ms.topology='2' "
											+ "MERGE (circuit:circuit {igpnet:{igpnet}} ) "
											+ "ON CREATE SET circuit.topology='2.1',circuit.spopid={popid}, circuit.spopname={popname} "
											+ "ON MATCH SET circuit.topology='2.1',circuit.tpopid={popid}, circuit.tpopname={popname} "
											+ "MERGE (subint)-[l:CONNECTED]->(circuit) "
											+ "RETURN ID(mainint) as mainintID,ID(subint) as subintID", 
											Values.parameters("status", IgpNodes.get(key).getIgpinterfaces().get(igpint).getStatus(),
													"updater",updater,
													"currentdate", currentdate, 
													"ip", IgpNodes.get(key).getIgpRouterID().getHostAddress(), 
													"mainintname", mainintDesc, 
													"subintname", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc(), 
													"ifAlias", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfAlias(), 
													"ifIndex", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex(), 
													"popid", IgpNodes.get(key).getPopID(), 
													"popname", IgpNodes.get(key).getPopName(), 
													"igpnet", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfNet(),
													"status",  IgpNodes.get(key).getIgpinterfaces().get(igpint).getStatus(), 
													"message", "created by nmon via igp discovery"));
									intTx.success();
									while (intQueryResult.hasNext()) {
										Record record = intQueryResult.next();
										IgpNodes.get(key).getIgpinterfaces().get(igpint).setMainintDBID(Integer.toString(record.get("mainintID").asInt()));
										IgpNodes.get(key).getIgpinterfaces().get(igpint).setIntDBID(Integer.toString(record.get("subintID").asInt()));
										logger.debug("Maininterface DBID:" + IgpNodes.get(key).getIgpinterfaces().get(igpint).getMainintDBID() + " intDBID:" + IgpNodes.get(key).getIgpinterfaces().get(igpint).getIntDBID());
									}
									String intQuery = intQueryResult.consume().statement().toString();
									logger.debug("SubInterface creation Query is:" + intQuery);
									theOnesCreated = intQueryResult.consume().counters().nodesCreated();
									if (theOnesCreated == 1) {
										logger.debug("There were " + theOnesCreated + " the ones created.");
									} else {
										logger.debug("Node updated");
									}
									intTx.close();
								} catch (Exception transactionException) {
									logger.error("transaction error, while subinterface node adding" + transactionException.toString());
								}
							} else {
								try (Transaction intTx = session.beginTransaction() ) {
									StatementResult intQueryResult = intTx.run("MATCH (router:router { ip:{ip}}) "
											+ "MERGE (mainint:interface {nodeip:{ip},name:{mainintname}} ) "
											+ "ON CREATE SET mainint.type = 'mpls', mainint.statusupdater = {updater}, mainint.status={status}, mainint.message={message},mainint.alarmlevel = '0',mainint.statusupdatetime = {currentdate},mainint.topology='2',mainint.ifindex= {ifIndex},mainint.description = {ifAlias} "
											+ "MERGE (router)-[rm:CONTAIN]->(mainint) "
											+ "ON CREATE SET rm.status={status},rm.alarmlevel = '0',rm.statusupdater = {updater}, rm.statusupdatetime = {currentdate}, rm.topology='2' "
											+ "MERGE (circuit:circuit {igpnet:{igpnet}} ) "
											+ "ON CREATE SET circuit.topology='2.1',circuit.spopid={popid}, circuit.spopname={popname} "
											+ "ON MATCH SET circuit.topology='2.1',circuit.tpopid={popid}, circuit.tpopname={popname} "
											+ "MERGE (mainint)-[l:CONNECTED]->(circuit) "
											+ "RETURN ID(mainint) as mainintID", 
											Values.parameters("status", IgpNodes.get(key).getIgpinterfaces().get(igpint).getStatus(),
													"updater",updater,
													"currentdate", currentdate, 
													"ip", IgpNodes.get(key).getIgpRouterID().getHostAddress(), 
													"mainintname", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfDesc(), 
													"ifAlias", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfAlias(), 
													"ifIndex", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfIndex(),
													"popid", IgpNodes.get(key).getPopID(), 
													"popname",IgpNodes.get(key).getPopName(), 
													"igpnet", IgpNodes.get(key).getIgpinterfaces().get(igpint).getIfNet(),
													"status",  IgpNodes.get(key).getIgpinterfaces().get(igpint).getStatus(), 
													"message", "created by nmon via igp discovery"));
									intTx.success();          
									while (intQueryResult.hasNext()) {
										Record record = intQueryResult.next();
										IgpNodes.get(key).getIgpinterfaces().get(igpint).setIntDBID(Integer.toString(record.get("mainintID").asInt()));
										logger.debug("intDBID:" + IgpNodes.get(key).getIgpinterfaces().get(igpint).getIntDBID());
									}
									String intQuery = intQueryResult.consume().statement().toString();
									logger.debug("SubInterface creation Query is:" + intQuery);
									theOnesCreated = intQueryResult.consume().counters().nodesCreated();
									if (theOnesCreated == 1) {
										logger.debug("There were " + theOnesCreated + " the ones created.");
									} else {
										logger.debug("Node updated");
									}
									intTx.close();
								} catch (Exception transactionException) {
									logger.error("transaction error, while maininterface node adding" + transactionException.toString());
								}
							}


						}



					} catch (Exception transactionException) {
						logger.error("transaction error while interface creating or updating router" + transactionException.toString());
					}
				}
			} catch (Exception sessionException) {
				logger.error("cannot connect to Graph DB!, session error " + sessionException.toString());

			}
		}  catch (Exception driverException) {
			logger.error("cannot connect to Graph DB!, driver error" + driverException.toString());
		}

	}


	public Hashtable<String, IgpNode> getNodesToUpdate() {
		Hashtable<String, IgpNode> IgpNodestoUpdate = new Hashtable<String, IgpNode>();

		try (Driver driver = GraphDatabase.driver(this.connectionString, AuthTokens.basic(this.dbusername, this.dbpassword))){
			try (Session session = driver.session() ) {
				logger.info("connected to GraphDB!");
				try (Transaction tx = session.beginTransaction() ) {
					StatementResult QueryNodesToUpdate = tx.run("MATCH (n:router) WHERE n.updatestatus = {updatestatus} RETURN n.ip as ip,ID(n) as id, n.igpid as igpid", 
							Values.parameters("updatestatus", "updated"));
					tx.success();
					String Query = QueryNodesToUpdate.consume().statement().toString();
					logger.info("Query is:" + Query);

					while (QueryNodesToUpdate.hasNext()) {
						Record record = QueryNodesToUpdate.next();
						logger.debug("QueryNodesToUpdate " +  record.get("ip").asString());
						try {
							InetAddress igpRouterID = InetAddress.getByName(record.get("ip").asString());
							IgpNode nodetoupdate = new IgpNode(igpRouterID);
							nodetoupdate.setDbID(record.get("id").asInt());
							IgpNodestoUpdate.put(record.get("igpid").asString(),nodetoupdate);
						} catch (UnknownHostException e) {
							logger.error("returned ip " + record.get("ip").asString() + " is not well formed " + e.toString());
							continue;
						}
					}

				} catch (Exception transactionException) {
					logger.error("transaction error while interface creating or updating interface" + transactionException.toString());
					driver.close();
					return null;
				}
				driver.close();
				if (IgpNodestoUpdate.size() == 0) {
					return null;
				}

				return IgpNodestoUpdate;

			} catch (Exception sessionException) {
				logger.error("cannot connect to Graph DB!, session error " + sessionException.toString());
				return null;
			}
		}  catch (Exception driverException) {
			logger.error("cannot connect to Graph DB!, driver error" + driverException.toString());
			return null;
		}

	}

	public Hashtable<String, IgpNode> getIgpNodes() {
		Hashtable<String, IgpNode> IgpNodestoUpdate = new Hashtable<String, IgpNode>();

		try (Driver driver = GraphDatabase.driver(this.connectionString, AuthTokens.basic(this.dbusername, this.dbpassword))){
			try (Session session = driver.session() ) {
				logger.info("connected to GraphDB!");
				try (Transaction tx = session.beginTransaction() ) {
					StatementResult QueryNodesToUpdate = tx.run("MATCH (n:router) "
							+ "RETURN n.ip as ip,ID(n) as id, n.igpid as igpid,n.updatestatus as updatestatus, n.updatetime as updatetime");
					tx.success();		
					while (QueryNodesToUpdate.hasNext()) {
						Record record = QueryNodesToUpdate.next();
						logger.debug("QueryNodesToUpdate " +  record.get("ip").asString() + " updatestatus " + record.get("updatestatus").toString() + " updatetime " + record.get("updatetime").toString() );
						try {
							InetAddress igpRouterID = InetAddress.getByName(record.get("ip").asString());
							IgpNode nodetoupdate = new IgpNode(igpRouterID);
							nodetoupdate.setDbID(record.get("id").asInt());
							nodetoupdate.setUpdatestatus(record.get("updatestatus").asString());
							nodetoupdate.setUpdatetime(Long.parseLong(record.get("updatetime").toString()));
							IgpNodestoUpdate.put(record.get("igpid").asString(),nodetoupdate);							
						} catch (UnknownHostException e) {
							logger.error("returned ip " + record.get("ip").asString() + " is not well formed " + e.toString());
							continue;
						}
					}
					logger.debug("Query is:" + QueryNodesToUpdate.consume().statement().toString());
				} catch (Exception transactionException) {
					logger.error("transaction error while getting interface list to update" + transactionException.toString());
					driver.close();
					return null;
				}
				driver.close();
				if (IgpNodestoUpdate.size() == 0) {
					return null;
				}

				return IgpNodestoUpdate;

			} catch (Exception sessionException) {
				logger.error("cannot connect to Graph DB!, session error " + sessionException.toString());
				return null;
			}
		}  catch (Exception driverException) {
			logger.error("cannot connect to Graph DB!, driver error" + driverException.toString());
			return null;
		}

	}

	public Hashtable<String, IgpNode> getIgpInterfacesWithStatus(int status) {
		Hashtable<String, IgpNode> IgpNodes = new Hashtable<String, IgpNode>();

		try (Driver driver = GraphDatabase.driver(this.connectionString, AuthTokens.basic(this.dbusername, this.dbpassword))){
			try (Session session = driver.session() ) {
				logger.info("connected to GraphDB!");
				try (Transaction tx = session.beginTransaction() ) {
					StatementResult QueryNodesToUpdate = tx.run("MATCH (i:interface) WHERE i.status >= {status} RETURN ID(i) as id,i.name as name,i.ifindex as ifIndex,i.nodeip as nodeip", 
							Values.parameters("status", status));
					tx.success();
					while (QueryNodesToUpdate.hasNext()) {
						Record record = QueryNodesToUpdate.next();
						//Integer.toString(record.get("id").asInt())
						if (record.get("ifIndex").asString().equals("null")) continue;
						logger.debug("returned interface " + record.get("name").asString() + " ifIndex " + record.get("ifIndex").asString() + " DBID " + Integer.toString(record.get("id").asInt()));                    
						try {
							InetAddress igpRouterID = InetAddress.getByName(record.get(("nodeip")).asString());
							if (IgpNodes.containsKey(record.get(("nodeip")).toString())) {
								logger.debug(IgpNodes.get(record.get(("nodeip")).toString()).getIgpRouterID() + " is in the list");
								IgpInterface igpinterface = new IgpInterface(record.get("ifIndex").asString(),Integer.toString(record.get("id").asInt()),record.get("name").asString());
								IgpNodes.get(record.get(("nodeip")).toString()).getIgpinterfaces().put(record.get("ifIndex").asString(), igpinterface);
								logger.debug("interface " + record.get("name").asString() + " ifIndex " + record.get("ifIndex").asString() + " DBID " + Integer.toString(record.get("id").asInt()) + " added");
							} else {
								IgpNode nodetoupdate = new IgpNode(igpRouterID);
								IgpInterface igpinterface = new IgpInterface(record.get("ifIndex").asString(),Integer.toString(record.get("id").asInt()),record.get("name").asString());
								nodetoupdate.getIgpinterfaces().put(record.get("ifIndex").asString(), igpinterface);
								IgpNodes.put(record.get(("nodeip")).toString(), nodetoupdate);
								logger.debug("interface " + record.get("name").asString() + " ifIndex " + record.get("ifIndex").asString() + " DBID " + Integer.toString(record.get("id").asInt()) + " added");
							}
						} catch (UnknownHostException e) {
							logger.error("returned ip " + record.get("nodeip").asString() + " is not well formed " + e.toString());
							continue;
						}     

					}

				} catch (Exception transactionException) {
					logger.error("transaction error while interface creating or updating interface" + transactionException.toString());
					driver.close();
					return null;
				}
				driver.close();
				return IgpNodes;

			} catch (Exception sessionException) {
				logger.error("cannot connect to Graph DB!, session error " + sessionException.toString());
				return null;
			}
		}  catch (Exception driverException) {
			logger.error("cannot connect to Graph DB!, driver error" + driverException.toString());
			return null;
		}

	}

	public void setIgpInterfacesStatus(Hashtable<String, IgpInterface> igpinterfaces,String updater) {

		try (Driver driver = GraphDatabase.driver(this.connectionString, AuthTokens.basic(this.dbusername, this.dbpassword))){
			try (Session session = driver.session() ) {
				try (Transaction tx = session.beginTransaction() ) {
					for (String igpint : igpinterfaces.keySet()) {
						if (igpinterfaces.get(igpint).getStatus() >= 16) {
							logger.debug("creating interface update query for interface with dbid " + igpinterfaces.get(igpint).getIntDBID() + " and status " + igpinterfaces.get(igpint).getStatus());
							String currentdate;
							Date dt = new Date();
							SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							currentdate = sdf2.format(dt);
							StatementResult QueryNodesToUpdate = tx.run("MATCH (i:interface) WHERE ID(i) = {id} "
									+ "SET i.status = {status},i.statusupdater={updater},i.statusupdatetime={currentdate},i.message={message} RETURN i.status", 
									Values.parameters("id", Integer.parseInt(igpinterfaces.get(igpint).getIntDBID()),
											"status",igpinterfaces.get(igpint).getStatus(),
											"updater",updater,
											"currentdate", currentdate,
											"message", "updated by nmon up interface checker"));

							String intQuery = QueryNodesToUpdate.consume().statement().toString();
							logger.debug("Interface update Query is:" + intQuery);

						}
					}

					tx.success();
					tx.close();
				} catch (Exception transactionException) {
					logger.error("transaction error while interface creating or updating interface" + transactionException.toString());
					driver.close();
				}
				driver.close();
			} catch (Exception sessionException) {
				logger.error("cannot connect to Graph DB!, session error " + sessionException.toString());
			}
		}  catch (Exception driverException) {
			logger.error("cannot connect to Graph DB!, driver error" + driverException.toString());
		}

	}

	public void setIgpInterfacesStatus(Hashtable<String, IgpInterface> igpinterfaces,int status,String updater) {
		//subint.status={status},subint.statusupdater={updater},subint.statusupdatetime={currentdate}
		try (Driver driver = GraphDatabase.driver(this.connectionString, AuthTokens.basic(this.dbusername, this.dbpassword))){
			try (Session session = driver.session() ) {
				try (Transaction tx = session.beginTransaction() ) {
					for (String igpint : igpinterfaces.keySet()) {
						if (igpinterfaces.get(igpint).getStatus() < 16) {
							logger.debug("creating interface update query for interface with dbid " + igpinterfaces.get(igpint).getIntDBID() + " and status " + igpinterfaces.get(igpint).getStatus());
							String currentdate;
							Date dt = new Date();
							SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							currentdate = sdf2.format(dt);
							StatementResult QueryNodesToUpdate = tx.run("MATCH (i:interface) WHERE ID(i) = {id} "
									+ "SET i.status = CASE WHEN i.status >= 16 THEN {status} ElSE i.status = {status} END, i.statusupdater={updater},i.statusupdatetime={currentdate},i.message={message} RETURN i.status", 
									Values.parameters("id", Integer.parseInt(igpinterfaces.get(igpint).getIntDBID()),
											"status",status,
											"updater",updater,
											"currentdate", currentdate,
											"message", "updated by nmon down interface checker"));

							String intQuery = QueryNodesToUpdate.consume().statement().toString();
							logger.debug("Interface update Query is:" + intQuery);

						}
					}
					tx.success();
					tx.close();

				} catch (Exception transactionException) {
					logger.error("transaction error while interface creating or updating interface" + transactionException.toString());
					driver.close();
				}
				driver.close();
			} catch (Exception sessionException) {
				logger.error("cannot connect to Graph DB!, session error " + sessionException.toString());
			}
		}  catch (Exception driverException) {
			logger.error("cannot connect to Graph DB!, driver error" + driverException.toString());
		}

	}


	public Hashtable<String, Dslam> addOrUpdateDslams(Hashtable<String, Dslam> dslams,String updater) {
		try (Driver driver = GraphDatabase.driver(this.connectionString, AuthTokens.basic(this.dbusername, this.dbpassword))) {
			try (Session session = driver.session() ) {
				//Search remote PE in the database


				try (Transaction tx = session.beginTransaction()) {
					StatementResult result = null;
					for (String dslam : dslams.keySet()) {
						if (dslams.get(dslam).getPeIp() == null) {
							logger.warn("null remote pe ip for dslam " + dslams.get(dslam).getName());
						}
						result = tx.run("MATCH (router:router { ip:{ip}}) "
								+ "RETURN ID(router) as PeDbID, router.popid as PePopId, router.popname as PePopName",
								Values.parameters("ip", dslams.get(dslam).getPeIp().getHostAddress()));
						while (result.hasNext()) {
							Record record = result.next();
							dslams.get(dslam).setPedbId(record.get("PeDbID").asInt());
							dslams.get(dslam).setPePopId(record.get("PePopId").asInt());
							dslams.get(dslam).setPePopName(record.get("PePopName").asString());
							logger.debug("PEdbID:" + dslams.get(dslam).getPedbId() + " PePopId " + dslams.get(dslam).getPePopId() + " PePopName " + dslams.get(dslam).getPePopName() + " for dslam " + dslams.get(dslam).getIp().getHostAddress());
						}
					}			             			               
					tx.close();		
				}  catch (Exception transactionException) {
					logger.error("transaction error " + transactionException.toString());
					session.close();
					driver.close();
					return null;
				}

				try (Transaction tx = session.beginTransaction()) {
					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
					String currentdate;
					Date dt = new Date();
					SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					currentdate = sdf2.format(dt);
					StatementResult result = null;
					for (String dslam : dslams.keySet()) {
						try  {
						    dslams.get(dslam).getPedbId();
							if (dslams.get(dslam).getPeInt().contains(".")) {
								String mainintDesc = dslams.get(dslam).getPeInt().substring(0, dslams.get(dslam).getPeInt().indexOf("."));
								result = tx.run("MATCH (router:router { ip:{ip}}) "
										+ "MERGE (mainint:interface {nodeip:{ip},name:{mainintname}} ) "
										+ "ON CREATE SET mainint.message={message},mainint.status={status},mainint.statusupdater = {updater},mainint.statusupdatetime = {currentdate},mainint.topology='2' "
										+ "MERGE (subint:interface {nodeip:{ip},name:{subintname}} ) "
										+ "ON CREATE SET subint.message={message},subint.status={status},subint.statusupdater={updater},subint.statusupdatetime={currentdate},subint.topology='2' "
										+ "MERGE (router)-[rm:CONTAIN]->(mainint) "
										+ "ON MATCH SET rm.topology='2' "
										+ "ON CREATE SET rm.topology='2' "
										+ "MERGE (mainint)-[ms:CONTAIN]->(subint) "
										+ "ON CREATE SET ms.topology='2' "
										+ "MERGE (dslam:dslam {ip:{dslamip}} ) "
										+ "ON CREATE SET dslam.model='dslam',dslam.updater={updater},dslam.updatestatus='created',dslam.updatetime={timestamp},dslam.ip={dslamip},dslam.topology = '3.2',dslam.name={name},dslam.deviceid={deviceid},dslam.popid={popid},dslam.dblatitude={latitude},dslam.dblongitude={longitude},dslam.popname={popname},dslam.pepopid={pepopid}, dslam.pepopname={pepopname},dslam.status={status},dslam.message={message},dslam.statusupdater={updater},dslam.statusupdatetime={currentdate} "
										+ "ON MATCH SET  dslam.model='dslam',dslam.updater={updater},dslam.updatestatus='updated',dslam.updatetime={timestamp},dslam.ip={dslamip},dslam.topology = '3.2', dslam.name={name},dslam.deviceid={deviceid},dslam.popid={popid},dslam.dblatitude={latitude},dslam.dblongitude={longitude},dslam.popname={popname},dslam.pepopid={pepopid}, dslam.pepopname={pepopname} "
										+ "MERGE (dslamint:interface {nodeip:{dslamip},name:{dslamint}} ) "
										+ "ON CREATE SET dslamint.message={message},dslamint.status={status},dslamint.statusupdater = {updater},dslamint.statusupdatetime = {currentdate},dslamint.topology='2' "
										+ "MERGE (dslam)-[ulink:UPSTREAM]->(router) "
										+ "ON MATCH SET ulink.topology='3' "
										+ "ON CREATE SET ulink.topology='3' "
										+ "MERGE (dslam)-[di:CONTAIN]->(dslamint) "
										+ "ON MATCH SET di.topology='2' "
										+ "ON CREATE SET di.topology='2' "
										+ "MERGE (circuit:circuit {igpnet:{dslamip}} ) "
										+ "ON CREATE SET circuit.topology='2.1',circuit.spopid={popid}, circuit.spopname={popname},circuit.tpopid={pepopid}, circuit.tpopname={pepopname} "
										+ "ON MATCH SET circuit.topology='2.1',circuit.tpopid={popid}, circuit.tpopname={popname},circuit.tpopid={pepopid}, circuit.tpopname={pepopname} "
										+ "MERGE (dslamint)-[dci:CONNECTED]->(circuit) "
										+ "ON MATCH SET dci.topology='2' "
										+ "ON CREATE SET dci.topology='2' "
										+ "MERGE (subint)-[sci:CONNECTED]->(circuit) "
										+ "ON MATCH SET sci.topology='2' "
										+ "ON CREATE SET sci.topology='2' "
										+ "RETURN ID(router) as PedbID, ID(dslam) as dslamdbid",
										Values.parameters("timestamp", timestamp.getTime(),
												"status", "0",
												"updater",updater,
												"currentdate", currentdate, 
												"ip", dslams.get(dslam).getPeIp().getHostAddress() ,
												"dslamip", dslams.get(dslam).getIp().getHostAddress(),
												"deviceid", dslams.get(dslam).getDeviceid(),
												"mainintname", mainintDesc,
												"subintname" , dslams.get(dslam).getPeInt(),
												"name" , dslams.get(dslam).getName(),
												"dslamint",dslams.get(dslam).getInt(),
												"popid", dslams.get(dslam).getPopID(), 
												"popname",dslams.get(dslam).getPopName(),
												"pepopid", dslams.get(dslam).getPePopId(), 
												"pepopname",dslams.get(dslam).getPePopName(), 
												"status",  0,
												"latitude", dslams.get(dslam).getLatitude(), 
												"longitude", dslams.get(dslam).getLongitude(), 
												"message", "created by nmon via dslam discovery"));
								while (result.hasNext()) {
									Record record = result.next();
									logger.debug("PEdbID:" + Integer.toString(record.get("PedbID").asInt()) + " for dslam " + dslams.get(dslam).getPeIp().getHostAddress() + " DBID "+ Integer.toString(record.get("dslamdbid").asInt()));
									dslams.get(dslam).setPedbId(record.get("PedbID").asInt());
									dslams.get(dslam).setDbID(record.get("dslamdbid").asInt());
								}
							} else {
								result = tx.run("MATCH (router:router { ip:{ip}}) "
										+ "MERGE (int:interface {nodeip:{ip},name:{intname}} ) "
										+ "ON CREATE SET int.message={message},int.status={status},int.statusupdater={updater},int.statusupdatetime={currentdate},int.topology='2' "
										+ "MERGE (router)-[rm:CONTAIN]->(int) "
										+ "ON MATCH SET rm.topology='2' "
										+ "ON CREATE SET rm.topology='2' "
										+ "ON CREATE SET rm.topology='2' "
										+ "MERGE (dslam:dslam {ip:{dslamip}} ) "
										+ "ON CREATE SET dslam.model='dslam',dslam.updater={updater},dslam.updatetime={timestamp},dslam.updatestatus='created',dslam.ip={dslamip},dslam.topology = '3.2',dslam.name={name},dslam.deviceid={deviceid},dslam.popid={popid},dslam.dblatitude={latitude},dslam.dblongitude={longitude},dslam.popname={popname},dslam.pepopid={pepopid}, dslam.pepopname={pepopname},dslam.status={status},dslam.message={message},dslam.statusupdater={updater},dslam.statusupdatetime={currentdate} "
										+ "ON MATCH SET  dslam.model='dslam',dslam.updater={updater},dslam.updatetime={timestamp},dslam.updatestatus='updated',dslam.ip={dslamip},dslam.topology = '3.2', dslam.name={name},dslam.deviceid={deviceid},dslam.popid={popid},dslam.dblatitude={latitude},dslam.dblongitude={longitude},dslam.popname={popname},dslam.pepopid={pepopid}, dslam.pepopname={pepopname} "
										+ "MERGE (dslamint:interface {nodeip:{dslamip},name:{dslamint}} ) "
										+ "ON CREATE SET dslamint.message={message},dslamint.status={status},dslamint.statusupdater = {updater},dslamint.statusupdatetime = {currentdate},dslamint.topology='2' "
										+ "MERGE (dslam)-[ulink:UPSTREAM]->(router) "
										+ "ON MATCH SET ulink.topology='3' "
										+ "ON CREATE SET ulink.topology='3' "
										+ "MERGE (dslam)-[di:CONTAIN]->(dslamint) "
										+ "ON MATCH SET di.topology='2' "
										+ "ON CREATE SET di.topology='2' "
										+ "MERGE (circuit:circuit {igpnet:{dslamip}} ) "
										+ "ON CREATE SET circuit.topology='2.1',circuit.spopid={popid}, circuit.spopname={popname},circuit.tpopid={pepopid}, circuit.tpopname={pepopname} "
										+ "ON MATCH SET circuit.topology='2.1',circuit.tpopid={popid}, circuit.tpopname={popname},circuit.tpopid={pepopid}, circuit.tpopname={pepopname} "
										+ "MERGE (dslamint)-[dci:CONNECTED]->(circuit) "
										+ "ON MATCH SET dci.topology='2' "
										+ "ON CREATE SET dci.topology='2' "
										+ "MERGE (int)-[sci:CONNECTED]->(circuit) "
										+ "ON MATCH SET sci.topology='2' "
										+ "ON CREATE SET sci.topology='2' "
										+ "RETURN ID(router) as PedbID, ID(dslam) as dslamdbid",
										Values.parameters("timestamp", timestamp.getTime(),
												"status", 0,
												"updater",updater,
												"currentdate", currentdate, 
												"ip", dslams.get(dslam).getPeIp().getHostAddress() ,
												"dslamip", dslams.get(dslam).getIp().getHostAddress(),
												"deviceid", dslams.get(dslam).getDeviceid(),
												"intname" , dslams.get(dslam).getPeInt(),
												"name" , dslams.get(dslam).getName(),
												"dslamint",dslams.get(dslam).getInt(),
												"popid", dslams.get(dslam).getPopID(), 
												"popname",dslams.get(dslam).getPopName(),
												"pepopid", dslams.get(dslam).getPePopId(), 
												"pepopname",dslams.get(dslam).getPePopName(), 
												"status", 0,
												"latitude", dslams.get(dslam).getLatitude(), 
												"longitude", dslams.get(dslam).getLongitude(), 
												"message", "created by nmon via dslam discovery"));
								while (result.hasNext()) {
									Record record = result.next();
									logger.debug("PedbID:" + Integer.toString(record.get("PedbID").asInt()) + " for dslam " + dslams.get(dslam).getPeIp().getHostAddress() + " DBID "+ Integer.toString(record.get("dslamdbid").asInt()));
									dslams.get(dslam).setPedbId(record.get("PedbID").asInt());
									dslams.get(dslam).setDbID(record.get("dslamdbid").asInt());
								}
							}						
						} catch (Exception e) {

						    dslams.get(dslam).getPedbId();
							logger.warn("unable to find remote pe " + dslams.get(dslam).getPeIp().getHostAddress() + " in the graph database " + dslams.get(dslam).getName());
							result = tx.run("MERGE (dslam:dslam {ip:{dslamip}} ) "
									+ "ON CREATE SET dslam.model='dslam',dslam.updater={updater},dslam.updatetime={timestamp},dslam.updatestatus='created',dslam.ip={dslamip},dslam.topology = '3.2',dslam.name={name},dslam.deviceid={deviceid},dslam.popid={popid},dslam.dblatitude={latitude},dslam.dblongitude={longitude},dslam.popname={popname},dslam.status={status},dslam.message={message},dslam.statusupdater={updater},dslam.statusupdatetime={currentdate} "
									+ "ON MATCH SET  dslam.model='dslam',dslam.updater={updater},dslam.updatetime={timestamp},dslam.updatestatus='updated',dslam.ip={dslamip},dslam.topology = '3.2', dslam.name={name},dslam.deviceid={deviceid},dslam.popid={popid},dslam.dblatitude={latitude},dslam.dblongitude={longitude},dslam.popname={popname} "
									+ "MERGE (dslamint:interface {nodeip:{dslamip},name:{dslamint}} ) "
									+ "ON CREATE SET dslamint.message={message},dslamint.status={status},dslamint.statusupdater = {updater},dslamint.statusupdatetime = {currentdate},dslamint.topology='2' "
									+ "MERGE (circuit:circuit {igpnet:{dslamip}} ) "
									+ "ON CREATE SET circuit.topology='2.1',circuit.spopid={popid}, circuit.spopname={popname} "
									+ "ON MATCH SET circuit.topology='2.1',circuit.tpopid={popid}, circuit.tpopname={popname} "
									+ "MERGE (dslamint)-[dci:CONNECTED]->(circuit) "
									+ "ON MATCH SET dci.topology='2' "
									+ "ON CREATE SET dci.topology='2' "
									+ "RETURN ID(dslam) as dslamdbid",
									Values.parameters("timestamp", timestamp.getTime(),
											"status", "0",
											"updater",updater,
											"currentdate", currentdate, 
											"dslamip", dslams.get(dslam).getIp().getHostAddress(),
											"deviceid", dslams.get(dslam).getDeviceid(),
											"name" , dslams.get(dslam).getName(),
											"dslamint",dslams.get(dslam).getInt(),
											"popid", dslams.get(dslam).getPopID(), 
											"popname",dslams.get(dslam).getPopName(),
											"status",  0,
											"latitude", dslams.get(dslam).getLatitude(), 
											"longitude", dslams.get(dslam).getLongitude(), 
											"message", "created by nmon via dslam discovery"));
							while (result.hasNext()) {
								Record record = result.next();
								logger.debug(" dslam " + dslams.get(dslam).getIp().getHostAddress() + " DBID "+ Integer.toString(record.get("dslamdbid").asInt()));
								dslams.get(dslam).setDbID(record.get("dslamdbid").asInt());
							}								 
						
						}
					}						
					tx.success();   
					tx.close();		
				}  catch (Exception transactionException) {
					logger.error("transaction error " + transactionException.toString());
					session.close();
					driver.close();
					return null;
				}				
			}  catch (Exception sessionException) {
				logger.error("cannot connect to Graph DB!, sessin error" + sessionException.toString());
				return null;
			}
			driver.close();
			return dslams;
		}  catch (Exception driverException) {
			logger.error("cannot connect to Graph DB!, driver error" + driverException.toString());
			return null;
		}

	}

	public String toString() {
		return "Neo4jDatabase [dbusername=" + this.dbusername + ", dbpassword=" + this.dbpassword + ", connectionString=" + this.connectionString;
	}
}

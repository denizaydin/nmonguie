package utilities;

import java.util.Hashtable;
import java.util.Iterator;

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



public class GuiCoordinateUtilitiy {
	private String dbusername;
	private String dbpassword;
	private String connectionString;
	
	  private static final Logger logger = LoggerFactory.getLogger("GuiCoordinateUtilitiy");

	public GuiCoordinateUtilitiy() {
	}

	public static void main(String[] args) {
		try ( Driver dbToUpdate = GraphDatabase.driver("bolt://10.2.24.161:7687", AuthTokens.basic("neo4j", "pet09tr&"))) {
			Session  dbToUpdateSession = dbToUpdate.session();
			logger.info("connected to Update GraphDB!");
            Hashtable<String, Node> nodestoupdate= new  Hashtable<String, Node>();
				try (Transaction updateNodestx = dbToUpdateSession.beginTransaction() ) {
					StatementResult GetNodesQuery = updateNodestx.run("MATCH (n:router) "
							+ "RETURN n.ip as ip, "
							+ "ID(n) as id");
					updateNodestx.success();
                    while (GetNodesQuery.hasNext()) {
                        Record SourceRecord = GetNodesQuery.next();
                        if (SourceRecord.get("ip").asString().equals("null")) continue;
                        String id = Integer.toString(SourceRecord.get("id").asInt());
                        Node node = new Node(SourceRecord.get("ip").asString(),id);
                        nodestoupdate.put(SourceRecord.get("ip").asString(), node);
                        logger.debug("QueryNodesToUpdate ID " + node.toString());
                    }
				} catch (Exception transactionException) {
					logger.error("GetNodesQuery " + transactionException.toString());
				}
				
				try ( Driver SourceDatabase = GraphDatabase.driver("bolt://10.2.24.157:7687", AuthTokens.basic("neo4j", "pet09tr&"))) {
				Session  SourceDatabasesSession = SourceDatabase.session();

				Iterator<String> iterators = nodestoupdate.keySet().iterator();
				while (iterators.hasNext()) {
					String ip = iterators.next();
					try (Transaction SourceDatabasetx = SourceDatabasesSession.beginTransaction() ) {
						StatementResult GetNodesQuery = SourceDatabasetx.run("MATCH (n:router) "
								+ "WHERE n.ip = {ip}"
								+ "RETURN "
								+ "n.guilatitude as guilatitude, "
								+ "n.guilongitude as guilongitude, "
								+ "n.x as x, "
								+ "n.popx as popx, "
								+ "n.y as y, "
								+ "n.popy as popy, "
								+ "n.ip as ip",
								Values.parameters("ip", ip));
						SourceDatabasetx.success();

		                    while (GetNodesQuery.hasNext()) {
		                        Record SourceRecord = GetNodesQuery.next();
		                        if (SourceRecord.get("ip").asString().equals("null")) continue;
		                        nodestoupdate.get(ip).getAttributes().put("x",SourceRecord.get("x").toString().replace("\"","\\\""));
		                        nodestoupdate.get(ip).getAttributes().put("y",SourceRecord.get("y").toString().replace("\"","\\\""));

		                        nodestoupdate.get(ip).getAttributes().put("popx",SourceRecord.get("popx").toString().replace("\"","\\\""));
		                        nodestoupdate.get(ip).getAttributes().put("popy",SourceRecord.get("popy").toString().replace("\"","\\\""));

		                        nodestoupdate.get(ip).getAttributes().put("guilatitude",SourceRecord.get("guilatitude").toString());
		                        nodestoupdate.get(ip).getAttributes().put("guilongitude",SourceRecord.get("guilongitude").toString());

		                        logger.debug("NodeParams ID " + nodestoupdate.get(ip).toString());
		                    }
					} catch (Exception transactionException) {
						logger.error("GetNodesQuery from source database" + transactionException.toString());
					}
				}
				
				SourceDatabase.close();
				} catch (Exception driverException) {
					logger.error("cannot connect Graph DB which we get parameters for nodes! " + driverException.getMessage());
				}
				
				try (Transaction updateNodestx = dbToUpdateSession.beginTransaction() ) {

				Iterator<String> iterators = nodestoupdate.keySet().iterator();
				while (iterators.hasNext()) {
					String ip = iterators.next();
					String updatestatement = null;
					 for (String attribute : nodestoupdate.get(ip).getAttributes().keySet()) {
						 attribute=attribute.replace("\"","\\\"");
						 logger.debug("node " + ip + " 	attribute " + attribute + " " + nodestoupdate.get(ip).getAttributes().get(attribute));
						 if (nodestoupdate.get(ip).getAttributes().get(attribute).equals("NULL")) continue;
						 if (updatestatement == null) {
							 updatestatement="SET n." + attribute + "="  +  nodestoupdate.get(ip).getAttributes().get(attribute) ;
						 } else {
							 updatestatement = updatestatement+ ", n." + attribute + "="  +  nodestoupdate.get(ip).getAttributes().get(attribute);
						 }
					 }
					 if (updatestatement == null) {
						 logger.warn("notting to update for node " + ip);
					 } else {
					 StatementResult GetNodesQuery = updateNodestx.run("MERGE (n:router {ip:$ip}) "
								+ "ON CREATE " + updatestatement + " "
								+ "ON MATCH " + updatestatement ,
								Values.parameters("ip", ip));
						String query = GetNodesQuery.consume().statement().toString();
						logger.debug("router query is:" + (String)query);

					 updateNodestx.success();
					 updateNodestx.close();
					 }
				}

				} catch (Exception transactionException) {
					logger.error("GetNodesQuery " + transactionException.toString());
				}
					
				
	
				
		
		dbToUpdate.close();
		} catch (Exception driverException) {
		logger.error("cannot connect Graph DB which will update! " + driverException.getMessage());
		}
		
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
}

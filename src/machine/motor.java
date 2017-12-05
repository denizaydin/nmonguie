package machine;

import dbinterface.InventoryDatabase;
import dbinterface.Neo4jDatabase;
import networkmonitor.CheckIgpDownInterfaces;
import networkmonitor.CheckIgpUpInterfaces;
import topologydiscovery.DslamDiscovery;
import topologydiscovery.MainIgpDiscovery;
import topologydiscovery.UpdateIgpNodes;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class motor
{
	private static final Logger logger = LoggerFactory.getLogger("main");


	public static void main(String[] args) { 
		Neo4jDatabase neo4jDatabase = new Neo4jDatabase();
		InventoryDatabase inventoryDatabase = new InventoryDatabase();
		String neo4jusername = null;
		String neo4jpassword = null;
		InetAddress neo4jipv4address = null;
		int neo4jboltport = 0;

		String invdbtype = null;
		String invdb = null;
		String invdbusername = null;
		String invdbpassword = null;
		InetAddress invdbipv4address = null;
		int invdbport = 0;
		String invselectstatement = null;
		String dslamselectstatement = null;
		InetAddress igpinformerIgpRouterId = null;


		Properties prop = new Properties();
		InputStream input = null;
		logger.info("Tring to read configuration file");
		try {
			input = new FileInputStream("config.properties");
			prop.load(input);
			if (prop.getProperty("neo4jusername") == null) {
				throw new IllegalArgumentException("Cannot Find neo4jusername in the configuration file");
			}
			neo4jusername = prop.getProperty("neo4jusername");

			if (prop.getProperty("neo4jpassword") == null) {
				throw new IllegalArgumentException("Cannot Find neo4jpassword in the configuration file");
			}
			neo4jpassword = prop.getProperty("neo4jpassword");

			neo4jipv4address = InetAddress.getByName(prop.getProperty("neo4jipv4address"));
			neo4jboltport = Integer.parseInt(prop.getProperty("neo4jboltport"));


			if (prop.getProperty("invdbtype").equals("mysql")) {
				invdbtype = prop.getProperty("invdbtype");
			} else {
				throw new IllegalArgumentException("Unsupported invdbtype " + prop.getProperty("invdbtype") + " found in the configuration file");
			}
			if (prop.getProperty("invdb") == null) {
				throw new IllegalArgumentException("Cannot Find invdb in the configuration file");
			}
			invdb = prop.getProperty("invdb");

			if (prop.getProperty("invdbusername") == null) {
				throw new IllegalArgumentException("Cannot Find invdbusername in the configuration file");
			}
			invdbusername = prop.getProperty("invdbusername");

			if (prop.getProperty("invdbpassword") == null) {
				throw new IllegalArgumentException("Cannot Find invdbpassword in the configuration file");
			}
			invdbpassword = prop.getProperty("invdbpassword");

			invdbipv4address = InetAddress.getByName(prop.getProperty("invdbipv4address"));
			invdbport = Integer.parseInt(prop.getProperty("invdbport"));
			if (prop.getProperty("invselectstatement") == null) {
				throw new IllegalArgumentException("Cannot Find invselectstatement in the configuration file");
			}
			invselectstatement = prop.getProperty("invselectstatement");
			dslamselectstatement = prop.getProperty("dslamselectstatement");

			igpinformerIgpRouterId = InetAddress.getByName(prop.getProperty("igpinformerigpid"));
		} catch (Exception ex) {
			logger.error(ex.toString());
		}


		logger.info("Finished reading config file");
		neo4jDatabase.configure(neo4jusername, neo4jpassword, neo4jipv4address, neo4jboltport);
		logger.info("Neo4jDatabase conntection configured");
		if (neo4jDatabase.test()) {
			logger.info("Testing connection to neo4j database, succeeded? ");
			inventoryDatabase.configure(invdbtype, invdbusername, invdbpassword, invdbipv4address, invdbport, invdb, invselectstatement);
			logger.info("InventoryDatabase conntection configured");
			if (inventoryDatabase.test()) {
				logger.info("Testing InventoryDatabase database, succeeded? ");
			} else {
				logger.error("Testing connection database, failed? ");
				System.exit(1);
			}


			MainIgpDiscovery topologydiscovery = new MainIgpDiscovery("igpdiscoverythread", igpinformerIgpRouterId, inventoryDatabase,neo4jDatabase);
			topologydiscovery.start();
		
			UpdateIgpNodes updateigpnodes = new UpdateIgpNodes("igpupdatethread",inventoryDatabase,neo4jDatabase,topologydiscovery);
			updateigpnodes.start();
				
			CheckIgpDownInterfaces checkigpdowninterfacestatus = new CheckIgpDownInterfaces("CheckDownInterfaces",inventoryDatabase,neo4jDatabase);
			checkigpdowninterfacestatus.start();
			
			CheckIgpUpInterfaces checkigpinterfacestatus = new CheckIgpUpInterfaces("CheckIgpUpInterfaces",inventoryDatabase,neo4jDatabase);
			checkigpinterfacestatus.start();
	
			
			inventoryDatabase.setDslamselectstatement(dslamselectstatement);
			DslamDiscovery dslamdiscovery = new DslamDiscovery("dslamdiscoverythread", inventoryDatabase,neo4jDatabase);
			dslamdiscovery.start();
			
		}
		else
		{
			logger.error("Testing connection database, failed? ");
			System.exit(1);
		}


	}
}
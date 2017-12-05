/**
 *
 */
package topologydiscovery;

import dbinterface.InventoryDatabase;
import dbinterface.Neo4jDatabase;


import java.util.Hashtable;


import models.Dslam;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





public class DslamDiscovery implements Runnable
{
	private Thread thread;
	private String threadName;
	private Boolean stop = Boolean.valueOf(false);

	private InventoryDatabase inventoryDatabase;
    private Neo4jDatabase neo4jDatabase;
	private Hashtable<String, Dslam> dslams;
	public boolean isRunning;

	private static final Logger logger = LoggerFactory.getLogger("topologydiscovery");

	public DslamDiscovery(String threadname,InventoryDatabase inventoryDatabase, Neo4jDatabase neo4jDatabase) {
		threadName = threadname;
		this.neo4jDatabase = neo4jDatabase;
		this.inventoryDatabase = inventoryDatabase;
		this.dslams = new Hashtable<String, Dslam>();

		logger.debug("created dslam discovery Thread, threadname " + threadName);
	}


	public void run()
	{
		logger.info("running IgpDiscovery with threadname " + threadName);
		
		try {
			while (!stop) {
			long waitTime=3600000L;
			this.isRunning = true;
			//get nodes from igp informer
			this.dslams = inventoryDatabase.getDslams();
			if (this.dslams != null) {
				
				neo4jDatabase.addOrUpdateDslams(this.dslams,"dslamdiscovery");
			logger.info("waiting for "+ waitTime + " miliseconds");
			this.isRunning = false;
			Thread.sleep(waitTime);
			} else {	
			logger.info("null dslam list returned waiting for 65000 miliseconds");
			this.isRunning = false;
			Thread.sleep(65000);
			}
			
	
			
			
			
			

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

}
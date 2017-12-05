package dbinterface;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;

import models.Dslam;
import models.IgpNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.Statement;



public class InventoryDatabase
{
  private String dbusername;
  private String dbpassword;
  private String url;
  private String dslamselectstatement;
  String selectstatement = null;
  
  private static final Logger logger = LoggerFactory.getLogger("database");
  
  public InventoryDatabase() {}
  
  public void configure(String dbtype, String dbusername, String dbpassword, InetAddress dbipv4address, int dbport, String dbname, String selectstatement) {
	this.dbusername = dbusername;
    this.dbpassword = dbpassword;
    url = ("jdbc:" + dbtype + ":/" + dbipv4address.toString() + ":" + dbport + "/" + dbname);
    this.selectstatement = selectstatement;
  }
  
  public boolean test() {
    logger.debug("tring to connect for getting node info");
    
    try {
      Class.forName("com.mysql.jdbc.Driver");
      Connection dbConnection = DriverManager.getConnection(this.url, this.dbusername, this.dbpassword);
      dbConnection.close();
      return true;
    } catch (SQLException e) {
      logger.error("Cannot Connect to Graph DB, url failed:" + e.getMessage());
      return false;
    } catch (ClassNotFoundException e) {
      logger.error("Where is your MySQL JDBC Driver?" + e.getMessage()); }
    return false;
  }
  

  public boolean getNode(IgpNode igpnode) {
    logger.debug("tring to get information for igpnode with routerid" + igpnode.getIgpRouterID());
    try {
      Class.forName("com.mysql.jdbc.Driver");
      Connection dbConnection = DriverManager.getConnection(this.url, this.dbusername, this.dbpassword);
      logger.debug("select statement is "+ this.selectstatement);
      PreparedStatement preparedStatement = dbConnection.prepareStatement(this.selectstatement);
      preparedStatement.setString(1, igpnode.getIgpRouterID().getHostAddress());
      logger.debug("prepared query is " + preparedStatement.toString());
      ResultSet rs = preparedStatement.executeQuery();
      if (rs.next()) {
        if (!rs.getString("snmpversion").equals("3")) {
          logger.warn("snmp version " + rs.getString("snmpversion") + " is not supported");
          igpnode.setInternalErrorStatus(true);
          igpnode.setInternalErrorMessage("snmp version " + rs.getString("snmpversion") + " is not supported" );
          dbConnection.close();
          return false;
        }
        logger.info("node found in the database");
        try {
          igpnode.setSnmpipv4address(InetAddress.getByName(rs.getString("snmpipv4address")));
        } catch (UnknownHostException e) {
          logger.error("snmpipv4address " + rs.getString("snmpipv4address") + " is not a ipv4 address");
          igpnode.setInternalErrorStatus(true);
          igpnode.setInternalErrorMessage("snmpipv4address " + rs.getString("snmpipv4address") + " is not a ipv4 address" );
          dbConnection.close();
          return false;
        }
        
        try {
            igpnode.setIgpRouterID(InetAddress.getByName(rs.getString("igprouterip")));
          } catch (UnknownHostException e) {
            logger.error("igprouterip " + rs.getString("igprouterip") + " is not a ipv4 address");
            igpnode.setInternalErrorStatus(true);
            igpnode.setInternalErrorMessage("igprouterip " + rs.getString("igprouterip") + " is not a ipv4 address");
            dbConnection.close();
            return false;
          }        
        igpnode.setHostname(rs.getString("hostname"));
        igpnode.setDeviceid(Integer.parseInt(rs.getString("id")));
        igpnode.setPopName(rs.getString("popname"));
        igpnode.setPopID(Integer.parseInt(rs.getString("popid")));
        igpnode.setLatitude(Float.parseFloat(rs.getString("latitude")));
        igpnode.setLongitude(Float.parseFloat(rs.getString("longitude")));
        
        
        igpnode.setSnmpUser(rs.getString("snmpusername"));
        igpnode.setSnmpPass(rs.getString("snmppassword"));
        igpnode.setSnmpPriv(rs.getString("snmpprivpassphrase"));
        igpnode.setSnmpVersion(rs.getString("snmpversion"));
        igpnode.setSnmpCommunity(rs.getString("snmpcommunity"));

        logger.info("snmp info for IgpNode:" + igpnode.getSnmpipv4address() + " snmpusername:" + igpnode.getSnmpUser() + " snmppassword:" + igpnode.getSnmpPass() + " snmpprivpassphrase:" + igpnode.getSnmpPriv() + 
          " popId:" + igpnode.getPopID() + " popName:" + igpnode.getPopName() + " latitude:" + igpnode.getLatitude() + " longitude:" + igpnode.getLongitude() + 
          " dbdeviceid:" + igpnode.getDeviceid());
        dbConnection.close();
        return true;
      }
      dbConnection.close();
    } catch (SQLException sqlexception) {
    	logger.error("sql error " + sqlexception.toString());
		return false;
    } catch (ClassNotFoundException classnotfoundexception) {
    	logger.error("database class error, check your database type!" + classnotfoundexception.toString());
    	return false;
    }
    logger.error("can not found igpnode in the inventory database");
    igpnode.setInternalErrorStatus(true);
    igpnode.setInternalErrorMessage("can not found igpnode in the inventory database");
    return false;
  }

  public  boolean getNodes(Hashtable<String, IgpNode> IgpNodes) {
	    try {
	      Class.forName("com.mysql.jdbc.Driver");
	      Connection dbConnection = DriverManager.getConnection(this.url, this.dbusername, this.dbpassword);
	      logger.debug("select statement is "+ this.selectstatement);
			logger.info("collecting information from nodes");
			Iterator<String> iterators = IgpNodes.keySet().iterator();
			while (iterators.hasNext()) {
				String key = (String)iterators.next();

			    logger.debug("tring to get information for igpnode with routerid" + IgpNodes.get(key).getIgpRouterID());

	      PreparedStatement preparedStatement = dbConnection.prepareStatement(this.selectstatement);
	      preparedStatement.setString(1, IgpNodes.get(key).getIgpRouterID().getHostAddress());
	      logger.debug("prepared query is " + preparedStatement.toString());
	      ResultSet rs = preparedStatement.executeQuery();
	      if (rs.next()) {
	        if (!rs.getString("snmpversion").equals("3")) {
	          logger.warn("snmp version " + rs.getString("snmpversion") + " is not supported");
	          IgpNodes.get(key).setInternalErrorStatus(true);
	          IgpNodes.get(key).setInternalErrorMessage("snmp version " + rs.getString("snmpversion") + " is not supported" );
	          continue;
	        }
	        logger.info("node found in the database");
	        try {
	        	IgpNodes.get(key).setSnmpipv4address(InetAddress.getByName(rs.getString("snmpipv4address")));
	        } catch (UnknownHostException e) {
	          logger.error("snmpipv4address " + rs.getString("snmpipv4address") + " is not a ipv4 address");
	          IgpNodes.get(key).setInternalErrorStatus(true);
	          IgpNodes.get(key).setInternalErrorMessage("snmpipv4address " + rs.getString("snmpipv4address") + " is not a ipv4 address" );
	          dbConnection.close();
	          continue;
	        }
	        
	        try {
	        	IgpNodes.get(key).setIgpRouterID(InetAddress.getByName(rs.getString("igprouterip")));
	          } catch (UnknownHostException e) {
	            logger.error("igprouterip " + rs.getString("igprouterip") + " is not a ipv4 address");
	            IgpNodes.get(key).setInternalErrorStatus(true);
	            IgpNodes.get(key).setInternalErrorMessage("igprouterip " + rs.getString("igprouterip") + " is not a ipv4 address");
	            continue;
	          }        
	        IgpNodes.get(key).setHostname(rs.getString("hostname"));
	        IgpNodes.get(key).setDeviceid(Integer.parseInt(rs.getString("id")));
	        IgpNodes.get(key).setPopName(rs.getString("popname"));
	        IgpNodes.get(key).setPopID(Integer.parseInt(rs.getString("popid")));
	        IgpNodes.get(key).setLatitude(Float.parseFloat(rs.getString("latitude")));
	        IgpNodes.get(key).setLongitude(Float.parseFloat(rs.getString("longitude")));
	        
	        
	        IgpNodes.get(key).setSnmpUser(rs.getString("snmpusername"));
	        IgpNodes.get(key).setSnmpPass(rs.getString("snmppassword"));
	        IgpNodes.get(key).setSnmpPriv(rs.getString("snmpprivpassphrase"));
	        IgpNodes.get(key).setSnmpVersion(rs.getString("snmpversion"));
	        IgpNodes.get(key).setSnmpCommunity(rs.getString("snmpcommunity"));

	        logger.debug("snmp info for IgpNode:" + IgpNodes.get(key).getSnmpipv4address() + " snmpusername:" + IgpNodes.get(key).getSnmpUser() + " snmppassword:" + IgpNodes.get(key).getSnmpPass() + " snmpprivpassphrase:" + IgpNodes.get(key).getSnmpPriv() + 
	          " popId:" + IgpNodes.get(key).getPopID() + " popName:" + IgpNodes.get(key).getPopName() + " latitude:" + IgpNodes.get(key).getLatitude() + " longitude:" + IgpNodes.get(key).getLongitude() + 
	          " dbdeviceid:" + IgpNodes.get(key).getDeviceid());

	      } else {
	  	    logger.error("can not found igpnode in the inventory database");
			iterators.remove();
			logger.warn("removed the node from the list");
	      }
		}
		  
	      dbConnection.close();
	    } catch (SQLException sqlexception) {
	    	logger.error("sql error " + sqlexception.toString());
			return false;
	    } catch (ClassNotFoundException classnotfoundexception) {
	    	logger.error("database class error, check your database type!" + classnotfoundexception.toString());
	    	return false;
	    }
	    	return false;
	  }

  public  Hashtable<String, Dslam> getDslams() {
	    try {
	      Class.forName("com.mysql.jdbc.Driver");
	      Connection dbConnection = DriverManager.getConnection(this.url, this.dbusername, this.dbpassword);
	      if (this.dslamselectstatement == null) {
	    	  logger.error("null dslam select statement");
	    	  return null;
	      }
	      logger.debug("select statement is "+ this.dslamselectstatement);
	      Statement statement = (Statement) dbConnection.createStatement();
	      ResultSet rs = statement.executeQuery(this.dslamselectstatement);
	      Hashtable<String, Dslam> dslams = new Hashtable<String, Dslam>();
	      while(rs.next()){
	    	  //String name,String deviceid,InetAddress ip, String popID,String popname
	    	    Dslam dslam;
		        try {		        	
			          String name  = rs.getString("DslamName");
			          int deviceid  = Integer.parseInt(rs.getString("DslamId"));
			          int popid  = Integer.parseInt(rs.getString("DslamPopId"));
			          InetAddress ip = InetAddress.getByName(rs.getString("DslamIp"));
			          String popname  = rs.getString("DslamPopName");		  
			    	  dslam = new Dslam(name,deviceid,ip,popid,popname);
			    	  logger.debug("added dslam " + dslam.toString());
		        } catch (Exception e) {
			          logger.error("Error while creating dslam, for parameters " + e.toString());
			          continue;
			    }
		        
		        try {
			    	dslam.setPeIp(InetAddress.getByName(rs.getString("PeIp")));
			    	dslam.setPeDeviceId(Integer.parseInt(rs.getString("PeId")));
			    	dslam.setPeInt(rs.getString("PeInt"));
		        } catch (Exception e) {
			          logger.error("Error while adding dslam PeIp, for parameters " + e.toString());
			    }
		        
		        try {
			    	dslam.setLatitude(Float.parseFloat(rs.getString("DslamLatitude")));
			    	dslam.setLongitude(Float.parseFloat(rs.getString("DslaLlongitude")));
			    	dslam.setInt(rs.getString("DslamInt"));
		        } catch (Exception e) {
			          logger.error("Error while adding dslam parameters " + e.toString());
			    }		        
		    	dslams.put(rs.getString("DslamId"), dslam);
	       }
	       rs.close();
	       statement.close();
	       dbConnection.close();
	       return dslams;
	    } catch (SQLException sqlexception) {
	    	logger.error("sql error " + sqlexception.toString());
			return null;
	    } catch (ClassNotFoundException classnotfoundexception) {
	    	logger.error("database class error, check your database type!" + classnotfoundexception.toString());
	    	return null;
	    }
	  }

  
  /**
 * @param dslamselectstatement the dslamselectstatement to set
 */
public void setDslamselectstatement(String dslamselectstatement) {
	this.dslamselectstatement = dslamselectstatement;
}

@Override
public String toString() {
	return "InventoryDatabase [dbusername=" + dbusername + ", dbpassword=" + dbpassword + ", url=" + url
			+ ", selectstatement=" + selectstatement + "]";
}
  
  
}
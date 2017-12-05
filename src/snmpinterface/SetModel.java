package snmpinterface;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeListener;

import models.IgpInterface;
import models.IgpNode;
import topologydiscovery.IgpOid;


public class SetModel implements TreeListener {
	private static final Logger logger = LoggerFactory.getLogger("snmp");
	   private boolean finished;
	    private IgpNode igpnode;
	    private IgpOid igpoid;
	    private long StartTime = Instant.now().getEpochSecond();
	    
	    public SetModel(IgpNode igpnode, IgpOid igpoid) {
	        this.igpnode = igpnode;
	        this.igpoid = igpoid;
	        logger.debug("created new listener for node " + this.igpnode.getSnmpTarget().getAddress() + " and oid " + this.igpoid.getOid().toString() +" StartTime:" + this.StartTime);
	        
	    }
	    
	    public void stopWalk() {
	        this.finished = true;
	        logger.debug("stopted snmp listener for node " + this.igpnode.getIgpRouterID() + " finished:" + this.finished);
	      	synchronized (this) {
				this.notify();
			}
	    }
	    public boolean isFinished() {
	        return this.finished;
	    }
	    public void finished(TreeEvent e) {
	        if (e.getVariableBindings() != null && e.getVariableBindings().length > 0) {
	            this.next(e);
	            this.igpoid.status = true;
	            logger.debug("completed snmp for node:" + this.igpnode.getIgpRouterID()  + " and for oid " + this.igpoid.getOid() +" total run time:" + this.getRunTime());
	            
	        }
	        if (e.isError()) {
	            logger.error("the following error occurred during for node:" + this.igpnode.getIgpRouterID() + " and for oid " + this.igpoid.getOid() + " " + e.getErrorMessage() + " total run time:" + this.getRunTime());
	            this.igpnode.setInternalErrorStatus(true);
	            this.igpnode.setInternalErrorMessage("snmp timeout while tring to set igpbase oid");

	        }
	        this.finished = true;
            this.igpoid.status = true;
	        logger.debug("stopted snmp listener for node:" + this.igpnode.getIgpRouterID() + " and for oid " + this.igpoid.getOid() + " finished:" + this.finished);
	      	synchronized (this) {
				this.notify();
			}
	    }
	    public boolean next(TreeEvent e) {
	        if (e.getVariableBindings() != null) {
				VariableBinding[] varBindings = e.getVariableBindings();
				for (VariableBinding varBinding : varBindings) {
	                if (varBinding != null) {
	                    String igpCircuitIndex;
	                    IgpInterface igpinterface;
	                    logger.trace("reply : " + this.igpnode.getIgpRouterID() + " " + varBinding.getOid().toString() + " : " + varBinding.getVariable().getSyntaxString() + " : " + varBinding.getOid().last() + " : " + varBinding.getVariable() + " : Value as String " + varBinding.getVariable().toString());
/*	                    if (varBinding.getOid().toString().startsWith("1.3.6.1.2.1.138.")) {
	                        this.igpnode.setIgpBaseOid(new OID("1.3.6.1.2.1.138"));
	                        logger.debug("baseoid:" + this.igpnode.getIgpBaseOid().toDottedString());
	                    } else if (varBinding.getOid().toString().startsWith("1.3.6.1.4.1.9.10.118.")) {
	                        this.igpnode.setIgpBaseOid(new OID("1.3.6.1.4.1.9.10.118"));
	                        logger.debug("baseoid:" + this.igpnode.getIgpBaseOid().toDottedString());
	                    }*/
	                    if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.1.1.2")) {
	                        this.igpnode.setIgpType(varBinding.getVariable().toInt());
	                        logger.debug("setted igptype for node:" + this.igpnode.getIgpRouterID() + " to:" + varBinding.getVariable().toInt());
	                    } else if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.1.1.3")) {
	                        String[] igpidarray = varBinding.getVariable().toString().split("\\:");
	                        String igpid = String.valueOf(igpidarray[0]) + igpidarray[1] + igpidarray[2] + igpidarray[3] + igpidarray[4] + igpidarray[5];
	                        this.igpnode.setIgpID(igpid);
	                        logger.debug("setted igpid for node:" + this.igpnode.getIgpRouterID() + " to:" + igpid);
	                    } else if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.1.1.9")) {
	                        this.igpnode.setIgpL2toL1Leaking(varBinding.getVariable().toInt());
	                        logger.debug("setted igpL2toL1Leaking for node:" + this.igpnode.getIgpRouterID() + " to:" + varBinding.getVariable().toInt());
	                    } else if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.3.2.1.2")) {
	                        igpCircuitIndex = varBinding.getOid().toString().replaceAll("(.*\\.)(\\d+)(\\.)(\\d+)(\\.)(\\d+)$", "$6");
	                        if (this.igpnode.getIgpinterfaces().get(igpCircuitIndex) == null) {
	                            igpinterface = new IgpInterface(igpCircuitIndex);
	                            this.igpnode.getIgpinterfaces().put(igpCircuitIndex, igpinterface);
	                            logger.debug("added igpinterface with igpCircuitIndex:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpCircuitIndex() + " for node:" + this.igpnode.getIgpRouterID() + " to:" + varBinding.getVariable().toInt());
	                        }
	                        this.igpnode.getIgpinterfaces().get(igpCircuitIndex).setIfIndex(varBinding.getVariable().toString());
	                        this.igpnode.getIfIndexToigpCircuitIndex().put(varBinding.getVariable().toString(), igpCircuitIndex);
	                        logger.debug("setted igpinterface ifIndex to:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIfIndex() + " for node:" + this.igpnode.getIgpRouterID());
	                    } else if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.3.2.1.6")) {
	                        igpCircuitIndex = varBinding.getOid().toString().replaceAll("(.*\\.)(\\d+)(\\.)(\\d+)(\\.)(\\d+)$", "$6");
	                        logger.debug("matched ciiCircType igpinterface with igpCircuitIndex:" + igpCircuitIndex + " for node:" + this.igpnode.getIgpRouterID());
	                        if (this.igpnode.getIgpinterfaces().get(igpCircuitIndex) == null) {
	                            igpinterface = new IgpInterface(igpCircuitIndex);
	                            this.igpnode.getIgpinterfaces().put(igpCircuitIndex, igpinterface);
	                            logger.debug("added igpinterface with igpCircuitIndex:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpCircuitIndex() + " for node:" + this.igpnode.getIgpRouterID() + " to:" + varBinding.getVariable().toInt());
	                        } else {
	                            logger.debug("igpinterface is already defined for node:" + this.igpnode.getIgpRouterID());
	                        }
	                        this.igpnode.getIgpinterfaces().get(igpCircuitIndex).setIgpInterfaceType(varBinding.getVariable().toString());
	                        logger.debug("setted igpinterface ciiCircType to:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpInterfaceType() + " for node:" + this.igpnode.getIgpRouterID());
	                    } else if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.3.2.1.8")) {
	                        igpCircuitIndex = varBinding.getOid().toString().replaceAll("(.*\\.)(\\d+)(\\.)(\\d+)(\\.)(\\d+)$", "$6");
	                        logger.debug("matched ciiCircType igpinterface with igpCircuitIndex:" + igpCircuitIndex + " for node:" + this.igpnode.getIgpRouterID());
	                        if (this.igpnode.getIgpinterfaces().get(igpCircuitIndex) == null) {
	                            igpinterface = new IgpInterface(igpCircuitIndex);
	                            this.igpnode.getIgpinterfaces().put(igpCircuitIndex, igpinterface);
	                            logger.debug("added igpinterface with igpCircuitIndex:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpCircuitIndex() + " for node:" + this.igpnode.getIgpRouterID() + " to:" + varBinding.getVariable().toInt());
	                        } else {
	                            logger.debug("igpinterface is already defined for node:" + this.igpnode.getIgpRouterID());
	                        }
	                        this.igpnode.getIgpinterfaces().get(igpCircuitIndex).setIgpLevel(varBinding.getVariable().toString());
	                        logger.debug("setted igpinterface ciiCircType to:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpLevel() + " for node:" + this.igpnode.getIgpRouterID());
	                    } else if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.3.2.1.9")) {
	                        igpCircuitIndex = varBinding.getOid().toString().replaceAll("(.*\\.)(\\d+)(\\.)(\\d+)(\\.)(\\d+)$", "$6");
	                        logger.debug("matched ciiCircType igpinterface with igpCircuitIndex:" + igpCircuitIndex + " for node:" + this.igpnode.getIgpRouterID());
	                        if (this.igpnode.getIgpinterfaces().get(igpCircuitIndex) == null) {
	                            igpinterface = new IgpInterface(igpCircuitIndex);
	                            this.igpnode.getIgpinterfaces().put(igpCircuitIndex, igpinterface);
	                            logger.debug("added igpinterface with igpCircuitIndex:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpCircuitIndex() + " for node:" + this.igpnode.getIgpRouterID() + " to:" + varBinding.getVariable().toInt());
	                        } else {
	                            logger.debug("igpinterface is already defined for node:" + this.igpnode.getIgpRouterID());
	                        }
	                        this.igpnode.getIgpinterfaces().get(igpCircuitIndex).setPassive(true);
	                        logger.debug("setted igpinterface ciiCircPassiveCircuit to:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).isPassive() + " for node:" + this.igpnode.getIgpRouterID());
	                    } else if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.4.1.1.3")) {
	                        igpCircuitIndex = varBinding.getOid().toString().replaceAll("(.*\\.)(\\d+)(\\.)(\\d+)(\\.\\d+)$", "$4");
	                        logger.debug("matched ciiCircLevelWideMetric igpinterface with igpCircuitIndex:" + igpCircuitIndex + " for node:" + this.igpnode.getIgpRouterID());
	                        if (this.igpnode.getIgpinterfaces().get(igpCircuitIndex) == null) {
	                            igpinterface = new IgpInterface(igpCircuitIndex);
	                            this.igpnode.getIgpinterfaces().put(igpCircuitIndex, igpinterface);
	                            logger.debug("added igpinterface with igpCircuitIndex:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpCircuitIndex() + " for node:" + this.igpnode.getIgpRouterID() + " to:" + varBinding.getVariable().toInt());
	                        } else {
	                            logger.debug("igpinterface is already defined for node:" + this.igpnode.getIgpRouterID());
	                        }
	                        this.igpnode.getIgpinterfaces().get(igpCircuitIndex).setIgpMetric(varBinding.getVariable().toString());
	                        logger.debug("setted igpinterface ciiCircLevelWideMetric to:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpMetric() + " for node:" + this.igpnode.getIgpRouterID());
	                    } else if (varBinding.getOid().toString().replaceAll(String.valueOf(this.igpnode.getIgpBaseOid().toString()) + "\\.", "").startsWith("1.6.3.1.3")) {
	                        igpCircuitIndex = varBinding.getOid().toString().replaceAll("(.*\\.)(\\d+)(\\.)(\\d+)(\\.)(\\d+)$", "$2");
	                        logger.debug("matched ciiISAdjIPAddrAddress igpinterface with igpCircuitIndex:" + igpCircuitIndex + " for node:" + this.igpnode.getIgpRouterID());
	                        if (this.igpnode.getIgpinterfaces().get(igpCircuitIndex) == null) {
	                            igpinterface = new IgpInterface(igpCircuitIndex);
	                            this.igpnode.getIgpinterfaces().put(igpCircuitIndex, igpinterface);
	                            logger.debug("added igpinterface with igpCircuitIndex:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIgpCircuitIndex() + " for node:" + this.igpnode.getIgpRouterID() + " to:" + varBinding.getVariable().toInt());
	                        } else {
	                            logger.debug("igpinterface is already defined for node:" + this.igpnode.getIgpRouterID());
	                        }
	                        this.igpnode.getIgpinterfaces().get(igpCircuitIndex).setIfNet(varBinding.getVariable().toString());
	                        logger.debug("setted igpinterface ciiISAdjIPAddrAddress to:" + this.igpnode.getIgpinterfaces().get(igpCircuitIndex).getIfNet() + " for node:" + this.igpnode.getIgpRouterID());
	                    }
	                }
	            }
	        }
	        return true;
	    }

	    public IgpNode getIgpnode() {
	        return this.igpnode;
	    }

	    public void setIgpnode(IgpNode IGPNODE) {
	        this.igpnode = IGPNODE;
	    }

	    public long getStartTime() {
	        return this.StartTime;
	    }

	    /**
		 * @return the oid
		 */
		public IgpOid getIgpOid() {
			return igpoid;
		}

		public long getRunTime() {
	        return Instant.now().getEpochSecond() - this.StartTime;
	    }
	}


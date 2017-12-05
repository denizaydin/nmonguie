package snmpinterface;


import models.IgpNode;
import java.time.Instant;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.VariableBinding;


public class SetInterfaceAttributes implements ResponseListener  {
    private static final Logger logger = LoggerFactory.getLogger("snmp");
    private IgpNode igpnode;
    private boolean finished = false;
    private long StartTime;
    
    public SetInterfaceAttributes(IgpNode igpnode) {
        this.igpnode = igpnode;
        this.StartTime = Instant.now().getEpochSecond();
        logger.debug("created new listener for target:" + this.igpnode.getSnmpTarget().getAddress() + " StartTime:" + this.StartTime);
    }
    
    
    public void onResponse(ResponseEvent event) {
		if (event.getResponse() != null) {
			Vector<? extends VariableBinding> vbs = event.getResponse().getVariableBindings();
            for (VariableBinding vb : vbs) {
                String ifindex;
                logger.debug("returned oid for target:" + this.igpnode.getSnmpTarget().getAddress() + " " + vb.toString() + " value:" + vb.getVariable());
                if (vb.getOid().toString().startsWith("1.3.6.1.2.1.2.2.1.2")) {
                    ifindex = vb.getOid().toString().replaceAll("(1.3.6.1.2.1.2.2.1.2.)(\\d+)$", "$2");
                    this.getIgpnode().getIgpinterfaces().get(ifindex).setIfDesc(vb.getVariable().toString());
                    logger.debug("node:" + this.igpnode.getIgpRouterID().getHostAddress() + " ifindex:" + vb.getOid().last() + " ifDesc:" + this.getIgpnode().getIgpinterfaces().get(ifindex).getIfDesc());
                    continue;
                }
                if (vb.getOid().toString().startsWith("1.3.6.1.2.1.2.2.1.9")) {
                    ifindex = vb.getOid().toString().replaceAll("(1.3.6.1.2.1.2.2.1.9.)(\\d+)$", "$2");
                    this.getIgpnode().getIgpinterfaces().get(ifindex).setIfLastChange(vb.getVariable().toString());
                    logger.debug("node:" + this.igpnode.getIgpRouterID().getHostAddress() + " ifindex:" + vb.getOid().last() + " ifLastChange:" + this.getIgpnode().getIgpinterfaces().get(ifindex).getIfLastChange());
                    continue;
                }
                if (vb.getOid().toString().startsWith("1.3.6.1.2.1.31.1.1.1.18")) {
                    ifindex = vb.getOid().toString().replaceAll("(1.3.6.1.2.1.31.1.1.1.18.)(\\d+)$", "$2");
                    this.getIgpnode().getIgpinterfaces().get(ifindex).setIfAlias(vb.getVariable().toString());
                    logger.debug("node:" + this.igpnode.getIgpRouterID().getHostAddress() + " ifindex:" + vb.getOid().last() + " IfAlias:" + this.getIgpnode().getIgpinterfaces().get(ifindex).getIfAlias());
                    continue;
                }
                if (vb.getOid().toString().startsWith("1.3.6.1.2.1.2.2.1.7")) {
                    ifindex = vb.getOid().toString().replaceAll("(1.3.6.1.2.1.2.2.1.7.)(\\d+)$", "$2");
                    this.getIgpnode().getIgpinterfaces().get(ifindex).setIfAdminStatus(vb.getVariable().toInt());
                    logger.debug("node:" + this.igpnode.getIgpRouterID().getHostAddress() + " ifindex:" + vb.getOid().last() + " IfAdminStatus:" + this.getIgpnode().getIgpinterfaces().get(ifindex).getIfAdminStatus());
                    continue;
                }
                if (vb.getOid().toString().startsWith("1.3.6.1.2.1.2.2.1.8")) {
                    ifindex = vb.getOid().toString().replaceAll("(1.3.6.1.2.1.2.2.1.8.)(\\d+)$", "$2");
                    this.getIgpnode().getIgpinterfaces().get(ifindex).setIfOperStatus(vb.getVariable().toInt());
                    logger.debug("node:" + this.igpnode.getIgpRouterID().getHostAddress() + " ifindex:" + vb.getOid().last() + " IfOperStatus:" + this.getIgpnode().getIgpinterfaces().get(ifindex).getIfOperStatus());
                    continue;
                }
                
                logger.warn("Undefined return snmp response for node:" + this.igpnode.getIgpID() + vb.getOid().toString());
            }
        }
        this.finished = true;
    	synchronized (this) {
				this.notify();
			}
    }

    public IgpNode getIgpnode() {
        return this.igpnode;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public long getRunTime() {
        return Instant.now().getEpochSecond() - this.StartTime;
    }
    public void stopWalk() {
        this.finished = true;
        logger.debug("stopted snmp listener for node:" + this.igpnode.getIgpRouterID() + " finished:" + this.finished);
      	synchronized (this) {
			this.notify();
		}
    }
}

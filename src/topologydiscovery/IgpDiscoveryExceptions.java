package topologydiscovery;

/**
 * Exception class for errors produced by creating or updating models.
 */

public class IgpDiscoveryExceptions extends Exception {
	private static final long serialVersionUID = 1L;
    /**
     * An error code indicating what went wrong. This field may take any of
     * these values:
     * <ul>
     * <li> {@link #IGP_INFORMER_ERROR}
     * </ul>
     * <p>
     * Depending on the value the opaqueData field may be set accordingly. If
     * so this is described below for each possible value.
     */
    protected int errorCode;
    /**
     * Information describing the error. The meaning of this field
     * as is described for each possible errorCode value.
     */
    protected String description = null;

    public static final int IGP_INFORMER_ERROR = -1;
    public static final int SNMP_TIMEOUT = -2;

	public IgpDiscoveryExceptions(int errorCode, String description ) {
		this.errorCode = errorCode;
		this.description = description;
	}
	
	public String toString() {
		switch (errorCode) {
        case IGP_INFORMER_ERROR:
            return "igpinformer error " + description;
        case SNMP_TIMEOUT:
            return "snmptimeout error " + description;
        default:
            return "Internal error " + errorCode;
        }
		}
	
	}


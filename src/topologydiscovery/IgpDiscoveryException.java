package topologydiscovery;

/**
 * Exception class for errors produced by igp dicovery.
 * <p>
 * JNCException uses an errorCode field to indicate what went wrong. Depending
 * on errorCode an opaqueData field may point to contextual information
 * describing the error. The toString method uses both of these fields to print
 * an appropriate error string describing the error.
 */
public class IgpDiscoveryException extends Exception {
	private static final long serialVersionUID = 1L;
    /**
     * An error code indicating what went wrong. This field may take any of
     * these values:
     * <ul>
     * <li> {@link #SNMP_ERROR}
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
    public static final int SNMP_ERROR = -1;

	public IgpDiscoveryException(int errorCode, String description ) {
		this.errorCode = errorCode;
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		switch (errorCode) {
        case SNMP_ERROR:
            return "snmp error " + description;
        default:
            return "Internal error: " + errorCode;
        }
		}
	
	}

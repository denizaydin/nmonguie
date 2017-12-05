package models;

/**
 * Exception class for errors produced by creating or updating models.
 */

public class ModelException extends Exception {
	private static final long serialVersionUID = 1L;
    /**
     * An error code indicating what went wrong. This field may take any of
     * these values:
     * <ul>
     * <li> {@link #SNMP_VERSION_ERROR}
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

    public static final int SNMP_VERSION_ERROR = -1;

	public ModelException(int errorCode, String description ) {
		this.errorCode = errorCode;
		this.description = description;
	}
	
	public String toString() {
		switch (errorCode) {
        case SNMP_VERSION_ERROR:
            return "snmp version error " + description;
        default:
            return "Internal error: " + errorCode;
        }
		}
	
	}


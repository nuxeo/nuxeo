package org.nuxeo.ecm.spaces.api.exceptions;

public class SpaceException extends Exception{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public SpaceException() {
		super();
	}

	public SpaceException(String message, Throwable cause) {
		super(message, cause);
	}

	public SpaceException(String message) {
		super(message);
	}

	public SpaceException(Throwable cause) {
		super(cause);
	}


}

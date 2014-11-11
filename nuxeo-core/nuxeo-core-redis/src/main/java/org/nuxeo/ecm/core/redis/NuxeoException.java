package org.nuxeo.ecm.core.redis;

import java.io.IOException;

public class NuxeoException extends RuntimeException {

	public NuxeoException(String message, IOException cause) {
		super(message, cause);
	}

	public NuxeoException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}

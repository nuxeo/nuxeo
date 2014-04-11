package org.nuxeo.ecm.core.api.local;

import org.nuxeo.ecm.core.api.ClientRuntimeException;

public class LocalException extends ClientRuntimeException {

    public LocalException(String message, Throwable t) {
        super(message, t);
    }

    public LocalException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;

}
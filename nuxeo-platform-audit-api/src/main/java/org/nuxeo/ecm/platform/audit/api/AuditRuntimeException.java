package org.nuxeo.ecm.platform.audit.api;

import org.nuxeo.ecm.core.api.ClientRuntimeException;

public class AuditRuntimeException extends ClientRuntimeException {

    private static final long serialVersionUID = -7230317313024997816L;
   
    public AuditRuntimeException(String message, Throwable t) {
        super(message, t);       
    }

    public AuditRuntimeException(String message) {
        super(message);    
    }

    
}

package org.nuxeo.wss;

public class WSSSecurityException extends WSSException {

    private static final long serialVersionUID = 1L;

    public WSSSecurityException(String message, Throwable t) {
        super(message, t);
    }

    public WSSSecurityException(String message) {
        super(message);
    }



}

package org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Exception for Marshaling errors
 * 
 * @author tiry
 * 
 */
public class PublishingMarshalingException extends ClientException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public PublishingMarshalingException(Throwable t) {
        super(t);
    }

    public PublishingMarshalingException(String message, Throwable t) {
        super(message, t);
    }

    public PublishingMarshalingException(String message) {
        super(message);
    }

}

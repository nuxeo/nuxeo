package org.nuxeo.ecm.core.event.jms;

import org.nuxeo.ecm.core.api.ClientException;

public class JMSBusNotActiveException extends ClientException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public JMSBusNotActiveException(Exception e) {
        super(e);
    }
}

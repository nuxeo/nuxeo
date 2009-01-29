package org.nuxeo.ecm.core.event.jms;

import org.nuxeo.ecm.core.api.ClientException;

public class CannotReconstructEventBundle extends ClientException {

    private static final long serialVersionUID = 1L;

    public CannotReconstructEventBundle(String message) {
        super(message);
    }

}

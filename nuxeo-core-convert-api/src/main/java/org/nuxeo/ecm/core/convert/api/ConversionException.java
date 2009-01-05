package org.nuxeo.ecm.core.convert.api;

import org.nuxeo.ecm.core.api.ClientException;

public class ConversionException extends ClientException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ConversionException(String message, Exception e) {
        super(message,e);
    }

    public ConversionException(String message) {
        super(message);
    }

}

package org.nuxeo.ecm.core.convert.api;

public class ConverterNotAvailable extends ConversionException {

    public ConverterNotAvailable(String message) {
        super("Converter " + message + " is not available");
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;


}

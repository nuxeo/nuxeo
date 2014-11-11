package org.nuxeo.ecm.core.convert.api;

public class ConverterNotAvailable extends ConversionException {

    private static final long serialVersionUID = 1L;

    public ConverterNotAvailable(String message) {
        super("Converter " + message + " is not available");
    }

}

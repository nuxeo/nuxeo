package org.nuxeo.ecm.core.convert.api;

/**
 * Exception thrown when selected converter is not registered.
 *
 * @author tiry
 */
public class ConverterNotRegistred extends ConversionException {

    private static final long serialVersionUID = 1L;

    public ConverterNotRegistred(String message) {
        super("Converter " + message + " is not registred");
    }

}

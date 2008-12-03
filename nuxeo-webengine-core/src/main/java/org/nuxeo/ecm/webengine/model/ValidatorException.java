package org.nuxeo.ecm.webengine.model;

public class ValidatorException extends Exception{

    /**
     *
     */
    private static final long serialVersionUID = -3215737999420033812L;

    public ValidatorException() {
        super();
    }

    public ValidatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidatorException(String message) {
        super(message);
    }

    public ValidatorException(Throwable cause) {
        super(cause);
    }

}

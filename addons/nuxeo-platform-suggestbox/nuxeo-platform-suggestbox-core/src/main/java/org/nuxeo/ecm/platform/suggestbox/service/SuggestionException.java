package org.nuxeo.ecm.platform.suggestbox.service;

/**
 * Exception raised when a suggester cannot perform it's suggestion due to
 * inconsistent configuration or problem when calling a backend service: in that
 * case the backend service exception should be wrapped as the cause.
 */
public class SuggestionException extends Exception {

    private static final long serialVersionUID = 1L;

    public SuggestionException(String msg) {
        super(msg);
    }

    public SuggestionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SuggestionException(Throwable cause) {
        super(cause);
    }
}

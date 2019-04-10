package org.nuxeo.ecm.platform.suggestbox.service;

/**
 * Exception thrown when a runtime component related to the SuggestionService
 * fails to initialize due to invalid configuration parameters or missing
 * requirements on the platform.
 */
public class ComponentInitializationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ComponentInitializationException(String msg) {
        super(msg);
    }

    public ComponentInitializationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ComponentInitializationException(Throwable cause) {
        super(cause);
    }
}

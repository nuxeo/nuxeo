package org.nuxeo.ecm.platform.suggestbox.service;

/**
 * Exception raised when the SuggestionService is unable to execute the selected
 * suggestion.
 */
public class SuggestionHandlingException extends Exception {

    private static final long serialVersionUID = 1L;

    public SuggestionHandlingException(String msg) {
        super(msg);
    }

    public SuggestionHandlingException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SuggestionHandlingException(Throwable cause) {
        super(cause);
    }

}

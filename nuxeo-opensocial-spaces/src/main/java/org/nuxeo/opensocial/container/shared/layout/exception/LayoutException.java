package org.nuxeo.opensocial.container.shared.layout.exception;

/**
 * @author <a href="mailto:vincent.dutat@ext.leroymerlin.fr">Vincent Dutat</a>
 */
public class LayoutException extends Exception {
    private static final long serialVersionUID = 1L;

    public LayoutException() {
    }

    public LayoutException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public LayoutException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public LayoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
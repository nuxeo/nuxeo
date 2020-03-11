package org.nuxeo.ecm.platform.comment.api.exceptions;

import org.nuxeo.ecm.core.api.NuxeoException;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

public class CommentNotFoundException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public CommentNotFoundException() {
        super();
        this.statusCode = SC_NOT_FOUND;
    }

    public CommentNotFoundException(String message) {
        super(message);
        this.statusCode = SC_NOT_FOUND;
    }

    public CommentNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = SC_NOT_FOUND;
    }

    public CommentNotFoundException(Throwable cause) {
        super(cause);
        this.statusCode = SC_NOT_FOUND;
    }

}

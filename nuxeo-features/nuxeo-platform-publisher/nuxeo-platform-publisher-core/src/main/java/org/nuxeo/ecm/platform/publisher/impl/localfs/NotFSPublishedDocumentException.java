package org.nuxeo.ecm.platform.publisher.impl.localfs;

public class NotFSPublishedDocumentException extends RuntimeException {

    public NotFSPublishedDocumentException() {
        super();
    }

    public NotFSPublishedDocumentException(String message) {
        super(message);
    }

    public NotFSPublishedDocumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFSPublishedDocumentException(Throwable cause) {
        super(cause);
    }

}

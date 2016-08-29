package org.nuxeo.ecm.directory;

/**
 * Exception thrown when attempting to delete a directory entry which has constraints preventing the deletion.
 *
 * @since 8.4
 */
public class DirectoryDeleteConstraintException extends DirectoryException {

    private static final long serialVersionUID = 1L;

    public DirectoryDeleteConstraintException() {
    }

    public DirectoryDeleteConstraintException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirectoryDeleteConstraintException(String message) {
        super(message);
    }

    public DirectoryDeleteConstraintException(Throwable cause) {
        super(cause);
    }

}

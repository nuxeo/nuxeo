package org.nuxeo.ecm.core.storage;

import org.nuxeo.ecm.core.api.NuxeoException;

public class FileSizeMismatchException extends NuxeoException {

    private static final long serialVersionUID = 1L;
    
    protected long length = 0;

    public FileSizeMismatchException(long length) {
        super();
        this.length = length;
    }

    public FileSizeMismatchException(long length, String message) {
        super(message);
        this.length = length;
    }

    /**
     * @return the length
     */
    public long getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(long length) {
        this.length = length;
    }

}

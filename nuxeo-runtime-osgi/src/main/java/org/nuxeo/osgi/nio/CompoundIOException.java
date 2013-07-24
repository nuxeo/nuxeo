package org.nuxeo.osgi.nio;

import java.io.IOException;

public class CompoundIOException extends IOException {

    private static final long serialVersionUID = 1L;

    public final IOException[] causes;

    public CompoundIOException(IOException[] causes) {
        this.causes = causes;
    }
}
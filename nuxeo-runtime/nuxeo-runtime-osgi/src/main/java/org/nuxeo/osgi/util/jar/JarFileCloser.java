package org.nuxeo.osgi.util.jar;

import java.io.IOException;
import java.util.jar.JarFile;

public interface JarFileCloser {

    void close(JarFile file) throws IOException;

    public static final JarFileCloser NOOP = new JarFileCloser() {

        @Override
        public void close(JarFile file) throws IOException {
            ;
        }
    };
}
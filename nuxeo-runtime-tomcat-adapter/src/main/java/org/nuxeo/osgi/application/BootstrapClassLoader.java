package org.nuxeo.osgi.application;

import java.net.URL;
import java.net.URLClassLoader;

public class BootstrapClassLoader extends URLClassLoader {

    BootstrapClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    protected void addURL(URL url) {
        super.addURL(url);
    }
}

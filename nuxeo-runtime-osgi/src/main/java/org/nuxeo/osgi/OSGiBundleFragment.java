package org.nuxeo.osgi;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class OSGiBundleFragment extends OSGiBundle {

    protected OSGiBundleHost host;

    protected OSGiBundleFragment(OSGiBundleFile file) throws BundleException {
        super(file);
    }

    public OSGiBundle getHost() {
        return host;
    }

    @Override
    public void start(int options) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(int options) throws BundleException {
        throw new UnsupportedOperationException();

    }

    @Override
    public void stop() throws BundleException {
        throw new UnsupportedOperationException();

    }

    @Override
    public URL getResource(String name) {
        throw new UnsupportedOperationException();

    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        throw new UnsupportedOperationException();

    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new UnsupportedOperationException();

    }

    @Override
    public BundleContext getBundleContext() {
        return null;
    }

    @Override
    protected String internalToString() {
        return super.internalToString() + ",host="+host;
    }

}

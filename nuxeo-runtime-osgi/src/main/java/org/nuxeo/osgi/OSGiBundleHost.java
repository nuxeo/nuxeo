package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.util.CompoundEnumeration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class OSGiBundleHost extends OSGiBundle {

    protected OSGiBundleContext context;

    protected double startupTime;

    protected File dataDir;

    protected final List<OSGiBundleFragment> fragments = new ArrayList<OSGiBundleFragment>();

    protected OSGiBundleHost(OSGiBundleFile file) throws BundleException {
        super(file);
    }

    protected Boolean allowHostOverride;

    protected boolean allowHostOverride() {
        if (allowHostOverride == null) {
            allowHostOverride = Boolean.parseBoolean(getHeader(
                    OSGiManifestReader.ALLOW_HOST_OVERRIDE, "false"));
        }
        return allowHostOverride.booleanValue();
    }

    protected Boolean isLazy;

    protected boolean isLazy() {
        if (isLazy == null) {
            isLazy = Constants.ACTIVATION_LAZY.equals(getHeader(
                    Constants.BUNDLE_ACTIVATIONPOLICY, "none"));
        }
        return isLazy.booleanValue();
    }

    protected String getActivatorClassName() {
        return headers == null ? null : headers.get(Constants.BUNDLE_ACTIVATOR);
    }

    @Override
    public void start(int options) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(int options) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BundleContext getBundleContext() {
        return context;
    }

    @Override
    public URL getResource(String name) {
        return context.loader.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return context.loader.getResources(name);
    }

    @Override
    public void start() throws BundleException {
        if ((state & (Bundle.STARTING | Bundle.ACTIVE)) != 0) {
            return;
        }
        setStarting();
        if (!isLazy()) {
            activate();
        } else {
            LogFactory.getLog(OSGiBundle.class).info(
                    "bundle " + this + " is starting");
        }
    }

    protected boolean isActivating = false;

    protected void activate() throws BundleException {
        if (isActivating) {
            return;
        }
        if ((state & Bundle.STARTING) == 0) {
            throw new BundleException("Bundle is not starting " + this);
        }
        isActivating = true;
        try {
            context.activate();
            setStarted();
            LogFactory.getLog(OSGiBundle.class).info(
                    "bundle " + this + " is started");
        } finally {
            isActivating = false;
        }
    }

    @Override
    public void stop() throws BundleException {
        setStopping();
        context.stop();
        setStopped();

    }

    protected void setResolved() throws BundleException {
        if (state != INSTALLED) {
            throw new IllegalStateException("Not in installed state (" + this
                    + ")");
        }
        state = RESOLVED;
        BundleEvent event = new BundleEvent(BundleEvent.RESOLVED, this);
        osgi.fireBundleEvent(event);
    }

    protected void setUnResolved() {
        if (state != RESOLVED) {
            throw new IllegalStateException("Not in resolved state (" + this
                    + ")");
        }
        state = INSTALLED;
        context = null;
        BundleEvent event = new BundleEvent(BundleEvent.UNRESOLVED, this);
        osgi.fireBundleEvent(event);
    }

    protected void setStarting() throws BundleException {
        if (state != RESOLVED) {
            throw new IllegalStateException("Not in resolved state (" + this
                    + ")");
        }
        state = STARTING;
        context = osgi.factory.newContext(this);
        context.start();
        BundleEvent event = new BundleEvent(BundleEvent.STARTING, this);
        osgi.fireBundleEvent(event);
    }

    protected void setStarted() {
        if (state != STARTING) {
            throw new IllegalStateException("Not in starting state (" + this
                    + ")");
        }
        state = ACTIVE;
        BundleEvent event = new BundleEvent(BundleEvent.STARTED, this);
        osgi.fireBundleEvent(event);
    }

    protected void setStopping() {
        if (state != ACTIVE) {
            throw new IllegalStateException("Not in active state (" + this
                    + ")");
        }
        state = STOPPING;
        BundleEvent event = new BundleEvent(BundleEvent.STOPPING, this);
        osgi.fireBundleEvent(event);
    }

    protected void setStopped() {
        if (state != STOPPING) {
            throw new IllegalStateException("Not in stopped state (" + this
                    + ")");
        }
        state = RESOLVED;
        BundleEvent event = new BundleEvent(BundleEvent.STOPPED, this);
        osgi.fireBundleEvent(event);
    }

    public double getStartupTime() {
        return startupTime;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if ((state & Bundle.STARTING) != 0) {
            try {
                activate();
            } catch (BundleException e) {
                throw new IllegalStateException("Cannot lazy activate " + this,
                        e);
            }
        }
        if ((state & Bundle.ACTIVE) == 0) {
            throw new ClassNotFoundException("Cannot load class " + name
                    + " from " + this + " (bundle is not active)");
        }
        return context.loader.loadClass(name);
    }

    protected static class CompoundEnumerationBuilder {

        protected final ArrayList<Enumeration<URL>> collected = new ArrayList<Enumeration<URL>>();

        public CompoundEnumerationBuilder add(Enumeration<URL> e) {
            collected.add(e);
            return this;
        }

        public Enumeration<URL> build() {
            return new CompoundEnumeration<URL>(
                    collected.toArray(new Enumeration[collected.size()]));
        }

    }

    @Override
    public URL getEntry(String name) {
        URL location = super.getEntry(name);
        if (location != null) {
            if (!allowHostOverride()) {
                return location;
            }
        }

        for (Bundle fragment : osgi.registry.getFragments(symbolicName)) {
            URL fragmentLocation = fragment.getEntry(name);
            if (fragmentLocation != null) {
                return fragmentLocation;
            }
        }

        return location;
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern,
            boolean recurse) {
        Enumeration<URL> hostEntries = super.findEntries(path, filePattern,
                recurse);
        Bundle[] fragments = osgi.registry.getFragments(symbolicName);
        if (fragments.length == 0) {
            return hostEntries;
        }

        CompoundEnumerationBuilder builder = new CompoundEnumerationBuilder();
        if (!allowHostOverride()) {
            builder.add(hostEntries);
        }

        for (Bundle fragment : fragments) {
            Enumeration<URL> fragmentEntries = fragment.findEntries(path,
                    filePattern, recurse);
            builder.add(fragmentEntries);
        }

        if (allowHostOverride()) {
            builder.add(hostEntries);
        }

        return builder.build();
    }

    @Override
    public <A> A adapt(Class<A> type) {
        if (type.isAssignableFrom(ClassLoader.class)) {
            return type.cast(context.loader);
        }
        throw new UnsupportedOperationException();
    }

    public OSGiLoader getLoader() {
        return context.loader;
    }
}

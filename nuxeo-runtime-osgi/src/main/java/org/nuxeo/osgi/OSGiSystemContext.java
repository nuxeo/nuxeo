package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class OSGiSystemContext extends OSGiBundleContext {

    protected final Log log = LogFactory.getLog(OSGiSystemContext.class);

    protected final List<FrameworkListener> frameworkListeners = new CopyOnWriteArrayList<FrameworkListener>();

    protected final List<BundleListener> bundleListeners = new CopyOnWriteArrayList<BundleListener>();

    protected final List<ServiceListener> serviceListeners = new CopyOnWriteArrayList<ServiceListener>();

    protected final Map<String, ServiceRegistration<?>> services = new HashMap<String, ServiceRegistration<?>>();

    protected final OSGiBundleRegistry registry = new OSGiBundleRegistry(this);

    protected final Properties config = new Properties();

    protected final File idTableFile;

    protected final File homeDir;

    protected final File dataDir;

    protected final File tmpDir;

    protected final File nestedDir;

    protected final String[] bootPackages;

    protected final OSGiFactory factory;

    protected OSGiAdapter adapter;

    public OSGiSystemContext(OSGiSystemBundle bundle, Properties props)
            throws BundleException {
        super(bundle);
        config.putAll(props);
        factory = newFactory();
        bootPackages = config.getProperty(
                OSGiAdapter.BOOT_DELEGATION, "java,javax,org.osgi,org.nuxeo.osgi").split(",");
        homeDir = newDir(OSGiAdapter.HOME_DIR,
                newOSGiTempFile(), "home");
        dataDir = newDir(OSGiAdapter.DATA_DIR, homeDir, "data");
        tmpDir = newDir(OSGiAdapter.TMP_DIR, homeDir, "tmp");
        removeContent(tmpDir);
        nestedDir = newDir(OSGiAdapter.NESTED_DIR, tmpDir, "nested");
        idTableFile = newIdTable(dataDir);
        registry.bundleIds.load(idTableFile);
        bundleListeners.add(registry);
    }


    protected File newOSGiTempFile() throws BundleException {
        try {
            return File.createTempFile("nxosgi", null);
        } catch (IOException e) {
            throw new BundleException("Cannot create temp osgi file", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected OSGiFactory newFactory() throws BundleException {
        String name = getProperty(OSGiAdapter.BUNDLE_FACTORY,
                OSGiDefaultFactory.class.getName());
        try {
            Class<? extends OSGiFactory> clazz;
            clazz = (Class<? extends OSGiFactory>) Class.forName(name);
            Constructor<? extends OSGiFactory> constructor = clazz.getDeclaredConstructor(OSGiSystemContext.class);
            return constructor.newInstance(this);
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            throw new BundleException("Cannot instanciate bundle factory "
                    + name, e);
        }
    }

    protected File newIdTable(File dir) {
        return new File(dir, "bundle.ids");
    }

    protected File newNestedDir(File dir) {
        File nestedDir = new File(dir, "nested-bundles");
        nestedDir.mkdirs();
        return nestedDir;
    }

    public boolean shouldScanForNestedJars() {
        return Boolean.valueOf(
                getProperty(OSGiAdapter.SCAN_FOR_NESTED_JARS, "false")).booleanValue();
    }

    @Override
    protected String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public String getProperty(String key) {
        Object value = config.get(key);
        if (value == null) {
            value = System.getProperty(key);
            if (value == null) {
                return null;
            }
            config.put(key, value);
        }
        return value.toString();
    }

    protected File newDir(String key, File base, String name) {
        String path = getProperty(key, new File(base, name).getPath());
        File dir = new File(path);
        dir.mkdirs();
        return dir;
    }

    protected void removeContent(File dir) throws BundleException {
        final Path base = dir.toPath();
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    if (exc == null) {
                        if (!base.equals(dir)) {
                            Files.delete(dir);
                        }
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw exc;
                    }
                }
            });
        } catch (IOException e) {
            throw new BundleException("Cannot remove content of " + dir, e);
        }
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        URI uri = URI.create(location);
        String scheme = uri.getScheme();
        switch (scheme) {
        case "file":
            String path = uri.getPath();
            return installBundle(new File(path));
        case "jar":
            throw new UnsupportedOperationException("TODO extract nested jar");
        default:
            throw new BundleException("Unsupported uri scheme " + scheme);
        }
    }

    protected OSGiBundle installBundle(File file) throws BundleException {
        OSGiBundleFile bf = factory.newFile(file.toPath());
        return installBundle(bf);
    }

    protected OSGiBundle installBundle(OSGiBundleFile file)
            throws BundleException {
        OSGiBundle b = factory.newBundle(file);
        if ("org.nuxeo.osgi".equals(b.getSymbolicName())) {
            return (OSGiBundle)osgi.getBundle();
        }
        registry.register(b);
        return b;
    }

    @Override
    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes,
            Object service, Dictionary<String, ?> properties) {
        @SuppressWarnings("rawtypes")
        OSGIServiceRegistration<?> reg = new OSGIServiceRegistration(this,
                bundle, clazzes, service);
        if (properties != null) {
            reg.setProperties(properties);
        }
        for (String c : clazzes) {
            services.put(c, reg);
        }
        return reg;
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service,
            Dictionary<String, ?> properties) {
        return registerService(new String[] { clazz }, service, properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz,
            S service, Dictionary<String, ?> properties) {
        return (ServiceRegistration<S>) registerService(
                new String[] { clazz.getName() }, service, properties);
    }

    @Override
    public boolean ungetService(ServiceReference<?> reference) {
        return false;
    }

    @Override
    public void removeBundleListener(BundleListener listener) {
        bundleListeners.remove(listener);
    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {
        frameworkListeners.remove(listener);
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        serviceListeners.remove(listener);
    }

    protected void uninstall(OSGiBundle bundle) throws BundleException {
        registry.unregister(bundle);
    }

    @Override
    public Bundle getBundle(String location) {
        return registry.getBundleByLocation(location);
    }

    protected void fireFrameworkEvent(FrameworkEvent event) {
        for (FrameworkListener listener : frameworkListeners) {
            try {
                listener.frameworkEvent(event);
            } catch (RuntimeException e) {
                log.error("Error during Framework Listener execution : "
                        + listener.getClass(), e);
            }
        }
    }

    protected void fireBundleEvent(BundleEvent event) {
        for (BundleListener listener : bundleListeners) {
            try {
                listener.bundleChanged(event);
            } catch (RuntimeException e) {
                log.error(
                        "Error during bundle execution : "
                                + listener.getClass(), e);
            }
        }
    }

    protected void fireServiceEvent(ServiceEvent event) {
        for (ServiceListener listener : serviceListeners) {
            try {
                listener.serviceChanged(event);
            } catch (RuntimeException e) {
                log.error(
                        "Error during service execution : "
                                + listener.getClass(), e);
            }
        }
    }

    protected OSGiBundleFile[] getNestedFiles(OSGiBundleFile file)
            throws BundleException {
        if (shouldScanForNestedJars()) {
            return file.findNestedBundles(this, tmpDir);
        } else {
            return file.getNestedBundles(this, tmpDir);
        }
    }

    protected ClassLoader originalClassloader;

    protected void setThreadContextClassLoader() {
        Thread thread = Thread.currentThread();
        originalClassloader = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
    }

    protected void resetThreadContextClassLoader() {
        Thread.currentThread().setContextClassLoader(originalClassloader);
        originalClassloader = null;
    }

    protected boolean matchBootPackage(String name) {
        for (String pkg : bootPackages) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}

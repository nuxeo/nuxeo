package org.nuxeo.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class OSGiLoader extends ClassLoader {

    protected Log log = LogFactory.getLog(OSGiLoader.class);

    protected final OSGiSystemContext osgi;

    protected final OSGiWiring wiring;

    protected final OSGiBundleContext context;

    protected static String path(String name) {
        return name.replace(".", "/").concat(".class");
    }

    protected OSGiLoader(OSGiBundleContext context, ClassLoader parent) {
        super(parent);
        this.context = context;
        osgi = context.bundle.osgi;
        wiring = new OSGiWiring(this);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        if ((context.bundle.state&(Bundle.ACTIVE|Bundle.STOPPING))== 0) {
            try {
                context.bundle.activate();
            } catch (BundleException e) {
                throw new ClassNotFoundException("Cannot activate " + context, e);
            }
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz;
            try {
                clazz = findClass(name);
            } catch (ClassNotFoundException e) {
                clazz = findParentClass(name);
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }

    protected Class<?> findParentClass(String name) throws ClassNotFoundException {
        ClassLoader parent = getParent();
        Class<?> clazz = parent.loadClass(name);
        if (parent instanceof OSGiLoader) {
            handleForeignLoadedClass(clazz);
        }
        return clazz;
    }

    @Override
    public String toString() {
        return "OSGiLoader [name=" + getName() + "]";
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = path(name);

        // in boot packages
        if (context.bundle.osgi.matchBootPackage(name)) {
            return getParent().loadClass(name);
        }

        // in this loader
        Class<?> clazz = findLocalClass(name, path);
        if (clazz != null) {
            return clazz;
        }

        // in indexed loaders
        Set<OSGiLoader> wired = wiring.mayContains(path);
        for (OSGiLoader loader : wired) {
            if (loader == this) {
                continue;
            }
            clazz = loader.findLocalClass(name, path);
            if (clazz != null) {
                return clazz;
            }
        }

        throw new ClassNotFoundException("No " + name + " available in "
                + this + " scope");
    }

    protected Class<?> findLocalClass(String name, String path) {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }
        Set<OSGiLoader> loaders = wiring.mayContains(path);
        if (!loaders.contains(this)) {
            return null; // in other loaders
        }
        URL location = getLocalFile(path);
        if (location == null) {
            return null;
        }
        byte[] data;
        try {
            InputStream in = location.openStream();
            try {
                data = IOUtils.toByteArray(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return null;
        }
        Class<?> clazz = defineClass(name, data, 0, data.length);
        return clazz;
    }

    @Override
    protected URL findResource(String path) {
        URL location = findLocalResource(path);
        if (location != null) {
            return location;
        }

        // search in index
        for (OSGiLoader loader : wiring.mayContains(path)) {
            if (loader == this) {
                continue;
            }
            location = loader.findLocalResource(path);
            if (location != null) {
                log.warn("requirement dep missing in " + this + " for " + loader
                        + "(" + path + ")");
                return location;
            }
        }
        return null;
    }

    protected URL findLocalResource(String path) {
        if (wiring.mayContains(path).contains(this)) {
            return null;
        }
        return getLocalFile(path);
    }

    @Override
    protected Enumeration<URL> findResources(final String path) throws IOException {
        final Iterator<OSGiLoader> loaders = new ArrayList<OSGiLoader>(wiring.mayContains(path)).iterator();
        return new Enumeration<URL>() {

            @Override
            public boolean hasMoreElements() {
                return loaders.hasNext();
            }

            @Override
            public URL nextElement() {
                return loaders.next().findLocalResource(path);
            }

        };
    }

    protected void wire() {
        wiring.load();
        osgi.loader.wiring.merge(wiring);
    }

    protected Enumeration<URL> listLocalFiles() {
        return context.bundle.findEntries("/", "*", true);
    }

    protected URL getLocalFile(String path) {
        return context.bundle.getEntry(path);
    }

    protected String getName() {
        return context.bundle.getSymbolicName();
    }

    protected String getPath() {
        return context.bundle.file.path.toString();
    }


    protected Class<?> handleForeignLoadedClass(Class<?> clazz) {
        ClassLoader other = clazz.getClassLoader();
        if (other != null && OSGiLoader.class.isAssignableFrom(other.getClass())) {
            log.warn("requirement missing in " + context.bundle + " for " + other
                    + " (" + clazz.getName() + ")");
            wiring.merge(((OSGiLoader)other).wiring);
        }
        return clazz;
    }


}
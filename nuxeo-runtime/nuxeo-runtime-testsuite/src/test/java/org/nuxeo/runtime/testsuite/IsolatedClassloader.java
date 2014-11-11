package org.nuxeo.runtime.testsuite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public class IsolatedClassloader extends ClassLoader {

    protected final ClassLoader baseLoader;

    protected final String[] packages;

    protected final Set<String> excludes = new HashSet<String>();

    public IsolatedClassloader(String... packages) {
        this(Thread.currentThread().getContextClassLoader(), packages);
    }

    public IsolatedClassloader(ClassLoader loader, String... packages) {
        super(null);
        baseLoader = loader;
        this.packages = packages;
    }

    public void exclude(Class<?>... classes) {
        for (Class<?> clazz:classes) {
            excludes.add(clazz.getName());
        }
    }

    @Override
    public URL findResource(String name) {
        return baseLoader.getResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return baseLoader.getResources(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        URL location = findResource(name.replace('.', '/').concat(".class"));
        if (location == null) {
            throw new ClassNotFoundException("Cannot find class " + name);
        }
        Class<?> c;
        try (InputStream is = location.openStream()) {
            final byte[] bytes = asBytes(is);
            c  = defineClass(name, bytes, 0, bytes.length);
        } catch (IOException cause) {
            throw new ClassNotFoundException("Cannot load class from " + location, cause);
        }
        if (c.getPackage() == null) {
            definePackage(name.substring(0, name.lastIndexOf('.')), null, null, null, null, null, null, null);
        }
        return c;
    }

    private byte[] asBytes(InputStream is) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4 << 10];
            while (true) {
                int n = is.read(buf);
                if (n < 0) {
                    return bos.toByteArray();
                } else if (n != 0) {
                    bos.write(buf, 0, n);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        if (!excludes.contains(name)) {
            for (String prefix : packages) {
                if (name.startsWith(prefix)) {
                    return super.loadClass(name, resolve);
                }
            }
        }
        Class<?> c = baseLoader.loadClass(name);
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

}
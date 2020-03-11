/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.loader.store;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The class loader allows modifying the stores (adding/removing). Mutable operations are thread safe.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ResourceStoreClassLoader extends ClassLoader implements Cloneable {

    private final Log log = LogFactory.getLog(ResourceStoreClassLoader.class);

    private volatile ResourceStore[] stores;

    private final LinkedHashSet<ResourceStore> cp; // class path

    public ResourceStoreClassLoader(final ClassLoader pParent) {
        this(pParent, new LinkedHashSet<ResourceStore>());
    }

    protected ResourceStoreClassLoader(final ClassLoader pParent, LinkedHashSet<ResourceStore> cp) {
        super(pParent);
        this.cp = cp;
        if (!cp.isEmpty()) {
            stores = cp.toArray(new ResourceStore[cp.size()]);
        }
    }

    public synchronized boolean addStore(ResourceStore store) {
        if (cp.add(store)) {
            stores = cp.toArray(new ResourceStore[cp.size()]);
            return true;
        }
        return false;
    }

    public synchronized boolean removeStore(ResourceStore store) {
        if (cp.remove(store)) {
            stores = cp.toArray(new ResourceStore[cp.size()]);
            return true;
        }
        return false;
    }

    @Override
    public synchronized ResourceStoreClassLoader clone() {
        return new ResourceStoreClassLoader(getParent(), new LinkedHashSet<>(cp));
    }

    public ResourceStore[] getStores() {
        return stores;
    }

    protected Class<?> fastFindClass(final String name) {
        ResourceStore[] _stores = stores; // use a local variable
        if (_stores != null) {
            for (final ResourceStore store : _stores) {
                final byte[] clazzBytes = store.getBytes(convertClassToResourcePath(name));
                if (clazzBytes != null) {
                    if (log.isTraceEnabled()) {
                        log.trace(getId() + " found class: " + name + " (" + clazzBytes.length + " bytes)");
                    }
                    doDefinePackage(name);
                    return defineClass(name, clazzBytes, 0, clazzBytes.length);
                }
            }
        }
        return null;
    }

    /**
     * Without this method getPackage() returns null
     *
     * @param name
     */
    protected void doDefinePackage(String name) {
        int i = name.lastIndexOf('.');
        if (i > -1) {
            String pkgname = name.substring(0, i);
            Package pkg = getPackage(pkgname);
            if (pkg == null) {
                definePackage(pkgname, null, null, null, null, null, null, null);
            }
        }
    }

    @Override
    protected URL findResource(String name) {
        ResourceStore[] _stores = stores; // use a local variable
        if (_stores != null) {
            for (final ResourceStore store : _stores) {
                final URL url = store.getURL(name);
                if (url != null) {
                    if (log.isTraceEnabled()) {
                        log.trace(getId() + " found resource: " + name);
                    }
                    return url;
                }
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        ResourceStore[] _stores = stores; // use a local variable
        if (_stores != null) {
            List<URL> result = new ArrayList<>();
            for (final ResourceStore store : _stores) {
                final URL url = store.getURL(name);
                if (url != null) {
                    if (log.isTraceEnabled()) {
                        log.trace(getId() + " found resource: " + name);
                    }
                    result.add(url);
                }
            }
            return Collections.enumeration(result);
        }
        return null;
    }

    @Override
    public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // log.debug(getId() + " looking for: " + name);
        Class<?> clazz = findLoadedClass(name);

        if (clazz == null) {
            clazz = fastFindClass(name);

            if (clazz == null) {

                final ClassLoader parent = getParent();
                if (parent != null) {
                    clazz = parent.loadClass(name);
                    // log.debug(getId() + " delegating loading to parent: " + name);
                } else {
                    throw new ClassNotFoundException(name);
                }

            } else {
                if (log.isDebugEnabled()) {
                    log.debug(getId() + " loaded from store: " + name);
                }
            }
        }

        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final Class<?> clazz = fastFindClass(name);
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        return clazz;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = findResources(name);
        if (urls == null) {
            final ClassLoader parent = getParent();
            if (parent != null) {
                urls = parent.getResources(name);
            }
        }
        return urls;
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url == null) {
            final ClassLoader parent = getParent();
            if (parent != null) {
                url = parent.getResource(name);
            }
        }
        return url;
    }

    protected String getId() {
        return "" + this + "[" + this.getClass().getClassLoader() + "]";
    }

    /**
     * org.my.Class -> org/my/Class.class
     */
    public static String convertClassToResourcePath(final String pName) {
        return pName.replace('.', '/') + ".class";
    }

}

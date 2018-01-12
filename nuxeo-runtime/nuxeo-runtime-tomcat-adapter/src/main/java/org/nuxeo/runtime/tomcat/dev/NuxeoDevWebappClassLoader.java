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
 *
 * $Id$
 */

package org.nuxeo.runtime.tomcat.dev;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import org.nuxeo.osgi.application.DevMutableClassLoader;
import org.nuxeo.runtime.tomcat.NuxeoWebappClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */

public class NuxeoDevWebappClassLoader extends NuxeoWebappClassLoader implements DevMutableClassLoader,
        WebResourcesCacheFlusher {

    /**
     * @since 9.3
     */
    @Override
    public void clearPreviousClassLoader() {
        clear();
    }

    /**
     * @since 9.3
     */
    @Override
    public void addClassLoader(URL... urls) {
        createLocalClassLoader(urls);
    }

    public LocalClassLoader createLocalClassLoader(URL... urls) {
        LocalClassLoader cl = new LocalURLClassLoader(urls, this);
        addChildren(cl);
        return cl;
    }

    protected DevFrameworkBootstrap bootstrap;

    protected List<LocalClassLoader> children;

    protected volatile LocalClassLoader[] _children;

    public NuxeoDevWebappClassLoader() {
        super();
        this.children = new ArrayList<LocalClassLoader>();
    }

    public NuxeoDevWebappClassLoader(ClassLoader parent) {
        super(parent);
        this.children = new ArrayList<LocalClassLoader>();
    }

    public void setBootstrap(DevFrameworkBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public DevFrameworkBootstrap getBootstrap() {
        return bootstrap;
    }

    public synchronized void addChildren(LocalClassLoader loader) {
        children.add(loader);
        _children = null;
    }

    public synchronized void clear() {
        children.clear();
        _children = null;
    }

    public synchronized void flushWebResources() {
        resourceEntries.clear();
        ResourceBundle.clearCache(this);
    }

    public LocalClassLoader[] getChildren() {
        LocalClassLoader[] cls = _children;
        if (cls == null) {
            synchronized (this) {
                _children = children.toArray(new LocalClassLoader[children.size()]);
                cls = _children;
            }
        }
        return cls;
    }

    /**
     * Do not synchronize this method at method level to avoid deadlocks.
     */
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            synchronized (this) {
                return super.loadClass(name, resolve);
            }
        } catch (ClassNotFoundException e) {
            for (LocalClassLoader cl : getChildren()) {
                try {
                    return cl.loadLocalClass(name, resolve);
                } catch (ClassNotFoundException ee) {
                    // do nothing
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (url != null) {
            return url;
        }
        for (LocalClassLoader cl : getChildren()) {
            url = cl.getLocalResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is != null) {
            return is;
        }
        for (LocalClassLoader cl : getChildren()) {
            try {
                is = cl.getLocalResourceAsStream(name);
            } catch (IOException e) {
                throw new RuntimeException("Cannot read input from " + name, e);
            }
            if (is != null) {
                return is;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        CompoundEnumeration<URL> enums = new CompoundEnumeration<URL>();
        enums.add(super.getResources(name));
        for (LocalClassLoader cl : getChildren()) {
            enums.add(cl.getLocalResources(name));
        }
        return enums;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public ClassLoader getParentClassLoader() {
        return parent;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this;
    }

    protected static class CompoundEnumeration<E> implements Enumeration<E> {

        private final List<Enumeration<E>> enums = new ArrayList<Enumeration<E>>();

        private int index = 0;

        public CompoundEnumeration() {
            // nothing to do
        }

        public CompoundEnumeration(List<Enumeration<E>> enums) {
            this.enums.addAll(enums);
        }

        private boolean next() {
            while (index < enums.size()) {
                if (enums.get(index) != null && enums.get(index).hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        @Override
        public boolean hasMoreElements() {
            return next();
        }

        @Override
        public E nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            return enums.get(index).nextElement();
        }

        public void add(Enumeration<E> e) {
            if (!e.hasMoreElements()) {
                return;
            }
            enums.add(e);
        }
    }

}

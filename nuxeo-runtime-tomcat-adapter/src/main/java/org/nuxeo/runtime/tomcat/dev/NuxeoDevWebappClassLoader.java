/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import java.util.ResourceBundle;

import org.nuxeo.runtime.tomcat.NuxeoWebappClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */

public class NuxeoDevWebappClassLoader extends NuxeoWebappClassLoader implements
        WebResourcesCacheFlusher {

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
        children = new ArrayList<LocalClassLoader>();
    }

    public NuxeoDevWebappClassLoader(ClassLoader parent) {
        super(parent);
        children = new ArrayList<LocalClassLoader>();
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

    public synchronized void removeChildren(ClassLoader loader) {
        children.remove(loader);
        _children = null;
    }

    public synchronized void clear() {
        children.clear();
        _children = null;
    }

    @Override
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
    public Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
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
    public InputStream getResourceAsStream(String name)  {
        InputStream is = super.getResourceAsStream(name);
        if  (is != null) {
            return is;
        }
        for (LocalClassLoader cl:getChildren()) {
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
        CompoundResourcesEnumerationBuilder builder = new CompoundResourcesEnumerationBuilder();
        builder.add(super.getResources(name));
        for (LocalClassLoader cl : getChildren()) {
            builder.add(cl.getLocalResources(name));
        }
        return builder.build();
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public void setParentClassLoader(ClassLoader pcl) {
        super.setParentClassLoader(pcl);
    }

    @Override
    public ClassLoader getParentClassLoader() {
        return parent;
    }


}

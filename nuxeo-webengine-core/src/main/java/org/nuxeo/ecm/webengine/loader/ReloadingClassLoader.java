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
 */
package org.nuxeo.ecm.webengine.loader;

import java.io.InputStream;
import java.net.URL;

import org.nuxeo.ecm.webengine.loader.store.ResourceStore;
import org.nuxeo.ecm.webengine.loader.store.ResourceStoreClassLoader;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ReloadingClassLoader extends ClassLoader {

    private final ClassLoader parent;
    private volatile ResourceStoreClassLoader delegate;

    public ReloadingClassLoader(final ClassLoader pParent) {
        super(pParent);
        parent = pParent;
        delegate = new ResourceStoreClassLoader(parent);
    }

    public void addResourceStore(final ResourceStore store) {
        delegate.addStore(store);
    }

    public boolean removeResourceStore(final ResourceStore store) {
        return delegate.removeStore(store);
    }

    public synchronized void reload() {
        delegate = delegate.clone();
    }

    @Override
    public void clearAssertionStatus() {
        delegate.clearAssertionStatus();
    }

    @Override
    public URL getResource(String name) {
        return delegate.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return delegate.getResourceAsStream(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return delegate.loadClass(name);
    }

    @Override
    public synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        return delegate.loadClass(name, resolve);
    }

    @Override
    public void setClassAssertionStatus(String className, boolean enabled) {
        delegate.setClassAssertionStatus(className, enabled);
    }

    @Override
    public void setDefaultAssertionStatus(boolean enabled) {
        delegate.setDefaultAssertionStatus(enabled);
    }

    @Override
    public void setPackageAssertionStatus(String packageName, boolean enabled) {
        delegate.setPackageAssertionStatus(packageName, enabled);
    }

}

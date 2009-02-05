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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.loader.store.ResourceStore;
import org.nuxeo.ecm.webengine.loader.store.ResourceStoreClassLoader;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ReloadingClassLoader extends ClassLoader {

    private final Log log = LogFactory.getLog(ReloadingClassLoader.class);

    private final ClassLoader parent;
    private final List<ResourceStore> stores;
    private ResourceStoreClassLoader delegate;

    public ReloadingClassLoader(final ClassLoader pParent) {
        super(pParent);
        parent = pParent;
        stores = new ArrayList<ResourceStore>();
        delegate = new ResourceStoreClassLoader(parent, new ResourceStore[0]);
    }

    public synchronized void addResourceStore(final ResourceStore store) throws Exception {
        stores.add(store);
        reload(); //need to reload to update usderlying store list
    }

    public synchronized boolean removeResourceStore(final ResourceStore store) {
        boolean ret = stores.remove(store);
        if (ret) {
            delegate = new ResourceStoreClassLoader(parent, stores.toArray(new ResourceStore[stores.size()]));
            return true;
        }
        return false;
    }

    public void reload() {
        delegate = new ResourceStoreClassLoader(parent, stores.toArray(new ResourceStore[stores.size()]));
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

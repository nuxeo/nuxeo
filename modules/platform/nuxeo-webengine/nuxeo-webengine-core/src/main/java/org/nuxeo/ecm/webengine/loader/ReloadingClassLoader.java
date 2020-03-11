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
    public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
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

/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.remoting;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.Property;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemoteComponentInstance implements ComponentInstance {

    final RemoteContext context;
    final ComponentName name;

    public RemoteComponentInstance(ComponentName name, RemoteContext context) {
        this.name = name;
        this.context = context;
    }

    @Override
    public void activate() throws Exception {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public void deactivate() throws Exception {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public void destroy() throws Exception {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public void reload() throws Exception {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public RuntimeContext getContext() {
        return context;
    }

    @Override
    public Object getInstance() {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public ComponentName getName() {
        return name;
    }

    @Override
    public Property getProperty(String property) {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public String[] getPropertyNames() {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public RuntimeContext getRuntimeContext() {
        return context;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public Object getPropertyValue(String property) {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public Object getPropertyValue(String property, Object defValue) {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }

    @Override
    public String[] getProvidedServiceNames() {
        throw new UnsupportedOperationException("Operation not supported for remote components");
    }
}

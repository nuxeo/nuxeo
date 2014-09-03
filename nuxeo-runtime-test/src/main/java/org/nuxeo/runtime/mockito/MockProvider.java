/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.runtime.mockito;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceProvider;

/**
 *
 *
 * @since 5.7.8
 */
public class MockProvider implements ServiceProvider {

    protected ServiceProvider next;

    protected final Map<Class<?>, Object> mocks = new HashMap<>();


    public MockProvider() {
    }

    public void bind(Class<?> klass, Object mock) {
        mocks.put(klass, mock);
    }

    public void clearBindings() {
        mocks.clear();
    }

    public void installSelf() {
        next = DefaultServiceProvider.getProvider();
        DefaultServiceProvider.setProvider(this);
    }

    public void uninstallSelf() {
        DefaultServiceProvider.setProvider(next);
        next = null;
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        if (mocks.containsKey(serviceClass)) {
            return serviceClass.cast(mocks.get(serviceClass));
        }
        if (next != null) {
            return next.getService(serviceClass);
        }
        return Framework.getRuntime().getService(serviceClass);
    }

}

/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.runtime.mockito;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceProvider;

/**
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

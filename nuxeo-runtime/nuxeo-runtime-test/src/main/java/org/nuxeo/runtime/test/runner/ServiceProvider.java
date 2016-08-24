/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.test.runner;

import org.nuxeo.runtime.api.Framework;

import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ServiceProvider<T> implements Provider<T> {

    protected final Class<T> clazz;

    public ServiceProvider(Class<T> clazz) {
        this.clazz = clazz;
    }

    public Class<T> getServiceClass() {
        return clazz;
    }

    @Override
    public T get() {
        try {
            return clazz.cast(Framework.getService(clazz));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get service: " + clazz, e);
        }
    }

    public Scope getScope() {
        return Scopes.NO_SCOPE;
    }

}

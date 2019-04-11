/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.runtime.api;

import java.util.Hashtable;
import java.util.Map;

/**
 * A service provider.
 * <p>
 * A service provider is used by the framework to be able to change the local services are found
 * <p>
 * For example, you may want to use a simple service provider for testing purpose to avoid loading the nuxeo runtime
 * framework to register services.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultServiceProvider implements ServiceProvider {

    private static ServiceProvider provider;

    public static void setProvider(ServiceProvider provider) {
        DefaultServiceProvider.provider = provider;
    }

    public static ServiceProvider getProvider() {
        return provider;
    }

    protected final Map<Class<?>, ServiceRef> registry = new Hashtable<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        ServiceRef ref = registry.get(serviceClass);
        if (ref != null) {
            return (T) ref.getService();
        }
        return null;
    }

    public <T> void registerService(Class<T> serviceClass, Class<?> implClass) {
        registry.put(serviceClass, new ServiceRef(implClass));
    }

    public <T> void registerService(Class<T> serviceClass, Object impl) {
        registry.put(serviceClass, new ServiceRef(impl));
    }

    public static class ServiceRef {
        final Class<?> type;

        Object service;

        public ServiceRef(Object service) {
            this.service = service;
            type = service.getClass();
        }

        public ServiceRef(Class<?> type) {
            service = null;
            this.type = type;
        }

        public Class<?> getType() {
            return type;
        }

        public Object getService() {
            if (service == null) {
                try {
                    service = type.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
            return service;
        }
    }

}

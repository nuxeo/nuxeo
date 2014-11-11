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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A service provider.
 * <p>
 * A service provider is used by the framework to be able to change the local
 * services are found
 * <p>
 * For example, you may want to use a simple service provider for testing
 * purpose to avoid loading the nuxeo runtime framework to register services.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultServiceProvider implements ServiceProvider {

    private static final Log log = LogFactory.getLog(DefaultServiceProvider.class);

    private static ServiceProvider provider;

    public static void setProvider(ServiceProvider provider) {
        DefaultServiceProvider.provider = provider;
    }

    public static ServiceProvider getProvider() {
        return provider;
    }

    protected final Map<Class<?>, ServiceRef> registry = new Hashtable<Class<?>, ServiceRef>();

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
                    service = type.newInstance();
                } catch (Exception e) {
                    log.error(e, e);
                }
            }
            return service;
        }
    }

}

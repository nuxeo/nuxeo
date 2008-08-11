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

package org.nuxeo.runtime.binding;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServiceManager implements org.nuxeo.runtime.ServiceManager {

    protected ServiceProvider[] providers;
    protected Map<String, Binding> bindings = new ConcurrentHashMap<String, Binding>();

    public ServiceManager() throws Exception  {
        this(new BeanServiceProvider(), new RuntimeServiceProvider());
    }

    public ServiceManager(ServiceProvider ... providers) {
        this.providers = providers;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) throws Exception {
        String bindingKey = serviceClass.getName();
        Binding binding = bindings.get(bindingKey);
        if (binding != null) {
            Object obj = binding.get();
            if (obj == null) {
                bindings.remove(bindingKey); // may be the service is no more online .. try other providers
            } else {
                return (T)obj;
            }
        }
        return (T)findService(serviceClass, bindingKey);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass, String key) throws Exception {
        String bindingKey = serviceClass.getName()+"@"+key;
        Binding binding = bindings.get(bindingKey);
        if (binding != null) {
            Object obj = binding.get();
            if (obj == null) {
                bindings.remove(bindingKey); // may be the service is no more online .. try other providers
            } else {
                return (T)obj;
            }
        }
        return (T)findService(serviceClass, bindingKey);
    }

    public Object findService(Class<?> serviceClass, String bindingKey) {
        for (ServiceProvider provider : providers) {
            Object obj = provider.getService(serviceClass, bindingKey);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    public void registerBinding(String name, Binding binding) {
        bindings.put(name, binding);
    }

    public void unregisterBinding(String name) {
        bindings.remove(name);
    }

    public void clearBindings() {
        bindings.clear();
    }

    public ServiceProvider[] getProviders() {
        return providers;
    }

    protected void destroyProviders() {
        if (this.providers != null) {
            for (ServiceProvider provider : this.providers) {
                provider.destroy();
            }
        }
        bindings = new ConcurrentHashMap<String, Binding>(); // clear bindings
    }

    public void setProviders(ServiceProvider ... providers) {
        destroyProviders();
        this.providers = providers;
    }

    public void addProvider(ServiceProvider provider) {
        bindings = new ConcurrentHashMap<String, Binding>(); // clear bindings
        if (providers == null) {
            providers = new ServiceProvider[] {provider};
        } else {
            ServiceProvider[] tmp = new ServiceProvider[providers.length+1];
            System.arraycopy(providers, 0, tmp, 0, providers.length);
            tmp[providers.length] = provider;
            providers = tmp;
        }
    }

}

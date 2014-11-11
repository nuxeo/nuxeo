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

import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RuntimeServiceProvider extends AbstractServiceProvider implements ComponentListener {

    protected RuntimeService runtime;
    protected Map<String, Binding> bindings;


    public RuntimeServiceProvider() {
        runtime = Framework.getRuntime();
        runtime.getComponentManager().addComponentListener(this);
        bindings = new ConcurrentHashMap<String, Binding>();
    }

    public void destroy() {
        runtime.getComponentManager().removeComponentListener(this);
        runtime = null;
        bindings = null;
    }

    /**
     * Named service lookup is not supported.
     */
    public Object getService(Class<?> serviceClass, String bindingKey) {
        ComponentInstance comp = runtime.getComponentManager().getComponentProvidingService(serviceClass);
        if (comp == null) {
            return null;
        }
        Object obj = comp.getAdapter(serviceClass);
        if (obj != null && manager != null) {
            // check if this is a singleton service
            Object obj2 = comp.getAdapter(serviceClass);
            if (obj == obj2) { // optimize bindigns for singleton services
                StaticBinding binding = new StaticBinding(bindingKey, obj);
                manager.registerBinding(bindingKey, binding);
                // register that binding to be able to nullify it if the service will be uninstalled later
                bindings.put(serviceClass.getName(), binding);
            } else { // this is not a singleton service
                Binding binding = new RuntimeServiceBinding(bindingKey, comp, serviceClass);
                manager.registerBinding(bindingKey, binding);
                bindings.put(serviceClass.getName(), binding);
            }
            return obj;
        }
        return null;
    }

    public void handleEvent(ComponentEvent event) {
        if (manager != null && event.id == ComponentEvent.COMPONENT_DEACTIVATED) {
            String[] services = event.registrationInfo.getProvidedServiceNames();
            if (services != null) {
                for (String service : services) {
                    Binding binding = bindings.remove(service);
                    if (binding != null) {
                        manager.unregisterBinding(binding.getKey());
                    }
                }
            }
        }
    }

}

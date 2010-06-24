/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.automation.server;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class AutomationServerComponent extends DefaultComponent implements
        AutomationServer {

    protected static final String XP_BINDINGS = "bindings";

    protected Map<String, RestBinding> bindings;

    protected volatile Map<String, RestBinding> lookup;

    public AutomationServerComponent() {

    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        bindings = new HashMap<String, RestBinding>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        bindings = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_BINDINGS.equals(extensionPoint)) {
            RestBinding binding = (RestBinding) contribution;
            addBinding(binding);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_BINDINGS.equals(extensionPoint)) {
            RestBinding binding = (RestBinding) contribution;
            removeBinding(binding);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (AutomationServer.class.isAssignableFrom(adapter)) {
            return adapter.cast(this);
        }
        return null;
    }

    public RestBinding getOperationBinding(String name) {
        return lookup().get(name);
    }

    public RestBinding getChainBinding(String name) {
        return lookup().get("Chain." + name);
    }

    public RestBinding[] getBindings() {
        Map<String, RestBinding> map = lookup();
        return map.values().toArray(new RestBinding[map.size()]);
    }

    protected String getBindingKey(RestBinding binding) {
        return binding.isChain() ? "Chain." + binding.getName()
                : binding.getName();
    }

    public synchronized void addBinding(RestBinding binding) {
        String key = getBindingKey(binding);
        bindings.put(key, binding);
        lookup = null;
    }

    public synchronized RestBinding removeBinding(RestBinding binding) {
        RestBinding result = bindings.remove(getBindingKey(binding));
        lookup = null;
        return result;
    }

    public boolean accept(String name, boolean isChain, HttpServletRequest req) {
        if (isChain) {
            name = "Chain." + name;
        }
        RestBinding binding = lookup().get(name);
        if (binding != null) {
            if (binding.isDisabled()) {
                return false;
            }
            if (binding.isSecure()) {
                if (!req.isSecure()) {
                    return false;
                }
            }
            Principal principal = req.getUserPrincipal();

            if (binding.isAdministrator() || binding.hasGroups()) {
                if (principal instanceof NuxeoPrincipal) {
                    NuxeoPrincipal np = (NuxeoPrincipal) principal;
                    if (binding.isAdministrator() && np.isAdministrator()) {
                        return true;
                    }
                    if (binding.hasGroups()) {
                        for (String group : binding.getGroups()) {
                            if (np.isMemberOf(group)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        }
        return true;
    }

    private final Map<String, RestBinding> lookup() {
        Map<String, RestBinding> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                lookup = new HashMap<String, RestBinding>(bindings);
                _lookup = lookup;
            }
        }
        return _lookup;
    }

}

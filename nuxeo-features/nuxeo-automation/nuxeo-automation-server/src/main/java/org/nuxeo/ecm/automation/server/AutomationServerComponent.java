/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation.server;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.io.services.IOComponent;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationServerComponent extends DefaultComponent implements AutomationServer {

    protected static final String XP_BINDINGS = "bindings";

    protected static final String IOCOMPONENT_NAME = "org.nuxeo.ecm.automation.io.services.IOComponent";

    protected IOComponent ioComponent;

    private static final String XP_MARSHALLER = "marshallers";

    protected Map<String, RestBinding> bindings;

    protected static final String XP_CODECS = "codecs";

    protected volatile Map<String, RestBinding> lookup;

    protected List<Class<? extends MessageBodyWriter<?>>> writers;

    protected List<Class<? extends MessageBodyReader<?>>> readers;

    @Override
    public void activate(ComponentContext context) {
        bindings = new HashMap<>();
        writers = new ArrayList<>();
        readers = new ArrayList<>();
        ioComponent = ((IOComponent) Framework.getRuntime().getComponentInstance(IOCOMPONENT_NAME).getInstance());
    }

    @Override
    public void deactivate(ComponentContext context) {
        bindings = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_BINDINGS.equals(extensionPoint)) {
            RestBinding binding = (RestBinding) contribution;
            addBinding(binding);
        } else if (XP_MARSHALLER.equals(extensionPoint)) {
            MarshallerDescriptor marshaller = (MarshallerDescriptor) contribution;
            writers.addAll(marshaller.getWriters());
            readers.addAll(marshaller.getReaders());
        } else if (XP_CODECS.equals(extensionPoint)) {
            ioComponent.registerContribution(contribution, extensionPoint, contributor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_BINDINGS.equals(extensionPoint)) {
            RestBinding binding = (RestBinding) contribution;
            removeBinding(binding);
        } else if (XP_CODECS.equals(extensionPoint)) {
            ioComponent.unregisterContribution(contribution, extensionPoint, contributor);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (AutomationServer.class.isAssignableFrom(adapter)) {
            return adapter.cast(this);
        }
        return null;
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        super.applicationStarted(context);
    }

    @Override
    public RestBinding getOperationBinding(String name) {
        return lookup().get(name);
    }

    @Override
    public RestBinding getChainBinding(String name) {
        return lookup().get(Constants.CHAIN_ID_PREFIX + name);
    }

    @Override
    public RestBinding[] getBindings() {
        Map<String, RestBinding> map = lookup();
        return map.values().toArray(new RestBinding[map.size()]);
    }

    protected String getBindingKey(RestBinding binding) {
        return binding.isChain() ? Constants.CHAIN_ID_PREFIX + binding.getName() : binding.getName();
    }

    @Override
    public synchronized void addBinding(RestBinding binding) {
        String key = getBindingKey(binding);
        bindings.put(key, binding);
        lookup = null;
    }

    @Override
    public synchronized RestBinding removeBinding(RestBinding binding) {
        RestBinding result = bindings.remove(getBindingKey(binding));
        lookup = null;
        return result;
    }

    @Override
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

    private Map<String, RestBinding> lookup() {
        Map<String, RestBinding> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                lookup = new HashMap<>(bindings);
                _lookup = lookup;
            }
        }
        return _lookup;
    }

    @Override
    public List<Class<? extends MessageBodyWriter<?>>> getWriters() {
        return writers;
    }

    @Override
    public List<Class<? extends MessageBodyReader<?>>> getReaders() {
        return readers;
    }

}

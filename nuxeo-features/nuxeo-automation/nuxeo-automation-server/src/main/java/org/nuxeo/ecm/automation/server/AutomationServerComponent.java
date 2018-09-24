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
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation.server;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationServerComponent extends DefaultComponent implements AutomationServer {

    /**
     * @since 10.3
     */
    public static final String XP_BINDINGS = "bindings";

    /**
     * @since 10.3
     */
    public static final String XP_MARSHALLER = "marshallers";

    protected List<Class<? extends MessageBodyWriter<?>>> writers = new ArrayList<>();

    protected List<Class<? extends MessageBodyReader<?>>> readers = new ArrayList<>();

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        List<MarshallerDescriptor> marshallers = getDescriptors(XP_MARSHALLER);
        marshallers.forEach(m -> {
            writers.addAll(m.writers);
            readers.addAll(m.readers);
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        writers.clear();
        readers.clear();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (AutomationServer.class.isAssignableFrom(adapter)) {
            return adapter.cast(this);
        }
        return null;
    }

    @Override
    public RestBinding getOperationBinding(String name) {
        return getDescriptor(XP_BINDINGS, name);
    }

    @Override
    public RestBinding getChainBinding(String name) {
        return getDescriptor(XP_BINDINGS, Constants.CHAIN_ID_PREFIX + name);
    }

    @Override
    public RestBinding[] getBindings() {
        List<RestBinding> descriptors = getDescriptors(XP_BINDINGS);
        return descriptors.toArray(new RestBinding[0]);
    }

    @Override
    public synchronized void addBinding(RestBinding binding) {
        register(XP_BINDINGS, binding);
    }

    @Override
    public synchronized RestBinding removeBinding(RestBinding binding) {
        return unregister(XP_BINDINGS, binding) ? binding : null;
    }

    @Override
    public boolean accept(String name, boolean isChain, HttpServletRequest req) {
        if (isChain) {
            name = Constants.CHAIN_ID_PREFIX + name;
        }
        RestBinding binding = getDescriptor(XP_BINDINGS, name);
        if (binding != null) {
            if (binding.isDisabled) {
                return false;
            }
            if (binding.isSecure && !req.isSecure()) {
                return false;
            }
            Principal principal = req.getUserPrincipal();

            if (binding.isAdministrator || binding.hasGroups()) {
                if (principal instanceof NuxeoPrincipal) {
                    NuxeoPrincipal np = (NuxeoPrincipal) principal;
                    if (binding.isAdministrator && np.isAdministrator()) {
                        return true;
                    }
                    if (binding.hasGroups()) {
                        for (String group : binding.groups) {
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

    @Override
    public List<Class<? extends MessageBodyWriter<?>>> getWriters() {
        return writers;
    }

    @Override
    public List<Class<? extends MessageBodyReader<?>>> getReaders() {
        return readers;
    }

}

/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */

package org.nuxeo.ecm.platform.io.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;
import org.nuxeo.ecm.platform.io.descriptors.IOResourceAdapterDescriptor;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component registering {@link IOResourceAdapter} instances to an {@link IOManager}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IOManagerComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(IOManagerComponent.class);

    public static final ComponentName NAME = new ComponentName(IOManagerComponent.class.getName());

    public static final String ADAPTERS_EP_NAME = "adapters";

    private IOManager service;

    @Override
    public void start(ComponentContext context) {
        service = new IOManagerImpl();
        this.<IOResourceAdapterDescriptor> getRegistryContributions(ADAPTERS_EP_NAME).forEach(desc -> {
            String name = desc.getName();
            try {
                IOResourceAdapter adapter = desc.getKlass().getDeclaredConstructor().newInstance();
                adapter.setProperties(desc.getProperties());
                service.addAdapter(name, adapter);
                log.info("IO resource adapter {} registered", name);
            } catch (ReflectiveOperationException e) {
                String msg = String.format("Error instantiating adapter '%s' (%s)", name, e.getMessage());
                addRuntimeMessage(Level.ERROR, msg);
                log.error(e, e);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        service = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(IOManager.class)) {
            return (T) service;
        }
        return null;
    }

}

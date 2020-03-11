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
 * $Id: IOManagerComponent.java 24959 2007-09-14 13:46:47Z atchertchian $
 */

package org.nuxeo.ecm.platform.io.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;
import org.nuxeo.ecm.platform.io.descriptors.IOResourceAdapterDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component registering {@link IOResourceAdapter} instances to an {@link IOManager}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IOManagerComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(IOManagerComponent.class.getName());

    public static final String ADAPTERS_EP_NAME = "adapters";

    private static final Log log = LogFactory.getLog(IOManagerComponent.class);

    private final IOManager service;

    public IOManagerComponent() {
        service = new IOManagerImpl();
    }

    public IOManager getIOManager() {
        return service;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(ADAPTERS_EP_NAME)) {
            IOResourceAdapterDescriptor desc = (IOResourceAdapterDescriptor) contribution;
            String name = desc.getName();
            String className = desc.getClassName();
            IOResourceAdapter adapter;
            try {
                // Thread context loader is not working in isolated EARs
                adapter = (IOResourceAdapter) IOManagerComponent.class.getClassLoader()
                                                                      .loadClass(className)
                                                                      .getDeclaredConstructor()
                                                                      .newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Caught error when instantiating adapter", e);
                return;
            }
            adapter.setProperties(desc.getProperties());
            IOResourceAdapter existing = service.getAdapter(name);
            if (existing != null) {
                log.warn(String.format("Overriding IO Resource adapter definition %s", name));
                service.removeAdapter(name);
            }
            service.addAdapter(name, adapter);
            log.info(String.format("IO resource adapter %s registered", name));
        } else {
            log.error(String.format("Unknown extension point %s, can't register !", extensionPoint));
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(ADAPTERS_EP_NAME)) {
            IOResourceAdapterDescriptor desc = (IOResourceAdapterDescriptor) contribution;
            service.removeAdapter(desc.getName());
        } else {
            log.error(String.format("Unknown extension point %s, can't unregister !", extensionPoint));
        }
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

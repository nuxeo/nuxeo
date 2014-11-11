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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: IOManagerComponent.java 24959 2007-09-14 13:46:47Z atchertchian $
 */

package org.nuxeo.ecm.platform.io.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;
import org.nuxeo.ecm.platform.io.descriptors.IOResourceAdapterDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component registering {@link IOResourceAdapter} instances to an
 * {@link IOManager}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IOManagerComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            IOManagerComponent.class.getName());

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
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(ADAPTERS_EP_NAME)) {
            IOResourceAdapterDescriptor desc = (IOResourceAdapterDescriptor) contribution;
            String name = desc.getName();
            String className = desc.getClassName();
            IOResourceAdapter adapter;
            try {
                // Thread context loader is not working in isolated EARs
                adapter = (IOResourceAdapter) IOManagerComponent.class.getClassLoader().loadClass(
                        className).newInstance();
            } catch (Exception e) {
                log.error("Caught error when instantiating adapter", e);
                return;
            }
            adapter.setProperties(desc.getProperties());
            try {
                IOResourceAdapter existing = service.getAdapter(name);
                if (existing != null) {
                    log.warn(String.format(
                            "Overriding IO Resource adapter definition %s",
                            name));
                    service.removeAdapter(name);
                }
                service.addAdapter(name, adapter);
                log.info(String.format("IO resource adapter %s registered",
                        name));
            } catch (ClientException e) {
                log.error("Error when registering IO resource adapter", e);
            }
        } else {
            log.error(String.format(
                    "Unknown extension point %s, can't register !",
                    extensionPoint));
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(ADAPTERS_EP_NAME)) {
            IOResourceAdapterDescriptor desc = (IOResourceAdapterDescriptor) contribution;
            try {
                service.removeAdapter(desc.getName());
            } catch (ClientException e) {
                log.error("Error when unregistering IO resource adapter", e);
            }
        } else {
            log.error(String.format(
                    "Unknown extension point %s, can't unregister !",
                    extensionPoint));
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

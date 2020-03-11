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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.adapter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentAdapterService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(ComponentName.DEFAULT_TYPE,
            "org.nuxeo.ecm.core.api.DocumentAdapterService");

    private static final Log log = LogFactory.getLog(DocumentAdapterService.class);

    /**
     * Document adapters
     */
    protected Map<Class<?>, DocumentAdapterDescriptor> adapters;

    public DocumentAdapterDescriptor getAdapterDescriptor(Class<?> itf) {
        return adapters.get(itf);
    }

    /**
     * @since 5.7
     */
    public DocumentAdapterDescriptor[] getAdapterDescriptors() {
        Collection<DocumentAdapterDescriptor> values = adapters.values();
        return values.toArray(new DocumentAdapterDescriptor[values.size()]);
    }

    public void registerAdapterFactory(DocumentAdapterDescriptor dae) {
        adapters.put(dae.getInterface(), dae);
        log.info("Registered document adapter factory " + dae);
    }

    public void unregisterAdapterFactory(Class<?> itf) {
        DocumentAdapterDescriptor dae = adapters.remove(itf);
        if (dae != null) {
            log.info("Unregistered document adapter factory: " + dae);
        }
    }

    @Override
    public void activate(ComponentContext context) {
        adapters = new ConcurrentHashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        adapters.clear();
        adapters = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("adapters")) {
            DocumentAdapterDescriptor dae = (DocumentAdapterDescriptor) contribution;
            registerAdapterFactory(dae);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("adapters")) {
            DocumentAdapterDescriptor dae = (DocumentAdapterDescriptor) contribution;
            unregisterAdapterFactory(dae.getInterface());
        }
    }

}

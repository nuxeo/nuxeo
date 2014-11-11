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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.adapter;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.model.PropertyFactory;
import org.nuxeo.ecm.core.api.model.impl.DefaultPropertyFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentAdapterService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            ComponentName.DEFAULT_TYPE, "org.nuxeo.ecm.core.api.DocumentAdapterService");

    private static final Log log = LogFactory.getLog(DocumentAdapterService.class);

    /**
     * Document adapters
     */
    protected Map<Class<?>, DocumentAdapterDescriptor> adapters;

    /**
     * Property factory are mapped using a key "schema:type" or "type"
     * if the factory is globally registered on all types having the name "type".
     * In that case the schema that declared the type is not important
     * The lookup is done by first looking for a "schema:type" entry and then
     * for a global "type" entry
     */
    protected Map<String, PropertyFactory> factories;


    public DocumentAdapterDescriptor getAdapterDescriptor(Class<?> itf) {
        return adapters.get(itf);
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

    public static void registerPropertyFactory(PropertyFactoryDescriptor descriptor) {
        try {
            DefaultPropertyFactory.getInstance().registerFactory(
                    descriptor.schema, descriptor.type,
                    (PropertyFactory) descriptor.klass.newInstance());
        } catch (Exception e) {
            log.error(
                    "Failed to instantiate the property type for "
                            + descriptor.schema + ':' + descriptor.type);
        }
    }

    public static void unregisterPropertyFactory(PropertyFactoryDescriptor descriptor) {
        DefaultPropertyFactory.getInstance().unregisterFactory(descriptor.schema, descriptor.type);
    }

    public PropertyFactory getPropertyFactory(String schema, String type) {
        String key = schema != null && schema.length() > 0 ? schema + ':' + type : type;
        PropertyFactory factory = factories.get(key);
        if (factory == null) {
            factory = factories.get(type);
        }
        return factory;
    }

    public PropertyFactory getPropertyFactory(String type) {
        return factories.get(type);
    }

    @Override
    public void activate(ComponentContext context) {
        factories = new Hashtable<String, PropertyFactory>();
        adapters = new Hashtable<Class<?>, DocumentAdapterDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        adapters.clear();
        adapters = null;
        factories.clear();
        factories = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        if (extensionPoint.equals("adapters")) {
            DocumentAdapterDescriptor dae = (DocumentAdapterDescriptor) contribution;
            registerAdapterFactory(dae);
        } else if (extensionPoint.equals("propertyFactories")) {
            PropertyFactoryDescriptor pfd = (PropertyFactoryDescriptor) contribution;
            registerPropertyFactory(pfd);
        } else if (extensionPoint.equals("sessionAdapters")) {
            SessionAdapterDescriptor desc = (SessionAdapterDescriptor) contribution;
            try {
                SessionAdapterFactory<?> factory = (SessionAdapterFactory<?>) desc.factory.newInstance();
                SessionAdapterFactory.registerAdapter(desc.itf, factory);
            } catch (Exception e) {
                log.error("Failed to register session adapter", e);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) {
        if (extensionPoint.equals("adapters")) {
            DocumentAdapterDescriptor dae = (DocumentAdapterDescriptor) contribution;
            unregisterAdapterFactory(dae.getInterface());
        } else if (extensionPoint.equals("propertyFactories")) {
            PropertyFactoryDescriptor pfd = (PropertyFactoryDescriptor) contribution;
            unregisterPropertyFactory(pfd);
        } else if (extensionPoint.equals("sessionAdapters")) {
            SessionAdapterDescriptor desc = (SessionAdapterDescriptor) contribution;
            SessionAdapterFactory.unregisterAdapter(desc.itf);
        }
    }

}

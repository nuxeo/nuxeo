/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.core.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@code InheritedPropertiesService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class InheritedPropertiesServiceImpl extends DefaultComponent implements
        InheritedPropertiesService {

    public static final String INHERITED_PROPERTIES_EP = "inheritedProperties";

    private static final Log log = LogFactory.getLog(InheritedPropertiesServiceImpl.class);

    protected Map<String, InheritedPropertiesDescriptor> inheritedPropertiesDescriptors;

    @Override
    public void activate(ComponentContext context) throws Exception {
        inheritedPropertiesDescriptors = new HashMap<String, InheritedPropertiesDescriptor>();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (INHERITED_PROPERTIES_EP.equals(extensionPoint)) {
            InheritedPropertiesDescriptor descriptor = (InheritedPropertiesDescriptor) contribution;
            if (inheritedPropertiesDescriptors.containsKey(descriptor.getSchema())) {
                log.info("Already  registered schema: "
                        + descriptor.getSchema() + ", overriding it.");
            }
            inheritedPropertiesDescriptors.put(descriptor.getSchema(),
                    descriptor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (INHERITED_PROPERTIES_EP.equals(extensionPoint)) {
            InheritedPropertiesDescriptor descriptor = (InheritedPropertiesDescriptor) contribution;
            inheritedPropertiesDescriptors.remove(descriptor.getSchema());
        }
    }

    public Map<String, InheritedPropertiesDescriptor> getInheritedPropertiesDescriptors() {
        return Collections.unmodifiableMap(inheritedPropertiesDescriptors);
    }

    public void inheritProperties(DocumentModel from, DocumentModel to)
            throws ClientException {
        for (InheritedPropertiesDescriptor descriptor : inheritedPropertiesDescriptors.values()) {
            inheritProperties(descriptor, from, to);
        }
    }

    protected void inheritProperties(InheritedPropertiesDescriptor descriptor,
            DocumentModel from, DocumentModel to) throws ClientException {
        String schema = descriptor.getSchema();
        if (from.hasSchema(schema) && to.hasSchema(schema)) {
            Map<String, Object> fromDataModel = from.getDataModel(schema).getMap();
            if (descriptor.allProperties()) {
                to.getDataModel(schema).setMap(fromDataModel);
            } else {
                inheritDefinedProperties(descriptor, fromDataModel, to);
            }
        }
    }

    protected void inheritDefinedProperties(
            InheritedPropertiesDescriptor descriptor,
            Map<String, Object> fromDataModel, DocumentModel to)
            throws ClientException {
        Map<String, Object> newMap = new HashMap<String, Object>();
        for (String property : descriptor.getProperties()) {
            Object value = fromDataModel.get(property);
            if (value != null) {
                newMap.put(property, value);
            }
        }
        to.getDataModel(descriptor.getSchema()).setMap(newMap);
    }

}

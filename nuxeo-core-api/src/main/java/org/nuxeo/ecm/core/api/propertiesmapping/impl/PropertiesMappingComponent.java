/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.api.propertiesmapping.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.propertiesmapping.PropertiesMappingContributionRegistry;
import org.nuxeo.ecm.core.api.propertiesmapping.PropertiesMappingDescriptor;
import org.nuxeo.ecm.core.api.propertiesmapping.PropertiesMappingService;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 * Service that allows to copy a set of metadata from a source to a target
 * document
 *
 * @since 5.6
 *
 */
public class PropertiesMappingComponent extends DefaultComponent implements
        PropertiesMappingService {

    public static final Log log = LogFactory.getLog(PropertiesMappingComponent.class);

    public static final String MAPPING_EP = "mapping";

    protected PropertiesMappingContributionRegistry mappingsRegistry = new PropertiesMappingContributionRegistry();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (MAPPING_EP.equals(extensionPoint)) {
            PropertiesMappingDescriptor desc = (PropertiesMappingDescriptor) contribution;
            mappingsRegistry.addContribution(desc);
        }
    }

    @Override
    public Map<String, String> getMapping(String mappingName) {
        return mappingsRegistry.getMappingProperties(mappingName);
    }

    @Override
    public void mapProperties(CoreSession session, DocumentModel sourceDoc,
            DocumentModel targetDoc, String mapping) throws ClientException {
        Map<String, String> properties = getMapping(mapping);
        for (String keyProp : properties.keySet()) {
            try {
                // verify that mapping can be done
                Property sourceProperty = sourceDoc.getProperty(keyProp);
                Property targetProperty = targetDoc.getProperty(properties.get(keyProp));

                Type sourceType = sourceProperty.getType();
                Type targetType = targetProperty.getType();

                if (!compatibleTypes(targetType, sourceType)) {
                    throw new ClientException(String.format(
                            "Invliad mapping.Can not map %s on type %s ",
                            sourceType, targetType));
                }

                targetDoc.setPropertyValue(targetProperty.getPath(),
                        sourceProperty.getValue());
            } catch (PropertyNotFoundException e) {
                // if invalid xpath
                throw new ClientException(e);
            }
        }
        session.saveDocument(targetDoc);
    }

    protected boolean compatibleTypes(Type targetType, Type sourceType) {
        if (!sourceType.getName().equals(targetType.getName())) {
            return false;
        }
        if (sourceType.isComplexType()) {
            for (Field field : ((ComplexType) sourceType).getFields()) {
                Field targetField = ((ComplexType) targetType).getField(field.getName());
                if (targetField == null
                        || !field.getType().equals(targetField.getType())) {
                    return false;
                }
            }
        }
        if (sourceType.isListType()) {
            if (!((ListType) sourceType).getFieldType().equals(
                    ((ListType) targetType).getFieldType())) {
                return false;
            }
            if (((ListType) sourceType).getFieldType().isComplexType()) {
                return compatibleTypes(((ListType) targetType).getFieldType(),
                        ((ListType) sourceType).getFieldType());
            }
        }
        return true;
    }
}
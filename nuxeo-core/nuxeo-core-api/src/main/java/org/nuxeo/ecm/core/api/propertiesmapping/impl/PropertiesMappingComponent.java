/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.api.propertiesmapping.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
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
 * Service that allows to copy a set of metadata from a source to a target document
 *
 * @since 5.6
 */
public class PropertiesMappingComponent extends DefaultComponent implements PropertiesMappingService {

    public static final Log log = LogFactory.getLog(PropertiesMappingComponent.class);

    public static final String MAPPING_EP = "mapping";

    protected PropertiesMappingContributionRegistry mappingsRegistry = new PropertiesMappingContributionRegistry();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
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
    public void mapProperties(CoreSession session, DocumentModel sourceDoc, DocumentModel targetDoc, String mapping)
            {
        Map<String, String> properties = getMapping(mapping);
        for (String keyProp : properties.keySet()) {
            // verify that mapping can be done
            Property sourceProperty = sourceDoc.getProperty(keyProp);
            Property targetProperty = targetDoc.getProperty(properties.get(keyProp));

            Type sourceType = sourceProperty.getType();
            Type targetType = targetProperty.getType();

            if (!compatibleTypes(targetType, sourceType)) {
                throw new NuxeoException(
                        String.format("Invalid mapping. Cannot map %s on type %s ", sourceType, targetType));
            }

            targetDoc.setPropertyValue(targetProperty.getXPath(), sourceProperty.getValue());
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
                if (targetField == null || !field.getType().equals(targetField.getType())) {
                    return false;
                }
            }
        }
        if (sourceType.isListType()) {
            if (!((ListType) sourceType).getFieldType().equals(((ListType) targetType).getFieldType())) {
                return false;
            }
            if (((ListType) sourceType).getFieldType().isComplexType()) {
                return compatibleTypes(((ListType) targetType).getFieldType(), ((ListType) sourceType).getFieldType());
            }
        }
        return true;
    }
}

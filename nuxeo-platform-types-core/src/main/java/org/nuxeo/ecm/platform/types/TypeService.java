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

package org.nuxeo.ecm.platform.types;

import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfiguration;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

public class TypeService extends DefaultComponent implements TypeManager {

    public static final ComponentName ID = new ComponentName(
            "org.nuxeo.ecm.platform.types.TypeService");

    private static final Log log = LogFactory.getLog(TypeService.class);

    public static String DEFAULT_CATEGORY = "misc";

    public static final String HIDDEN_IN_CREATION = "create";

    private TypeRegistry typeRegistry;

    @Override
    public void activate(ComponentContext context) {
        typeRegistry = new TypeRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        typeRegistry = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("types")) {
            typeRegistry.addContribution((Type) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("types")) {
            typeRegistry.removeContribution((Type) contribution);
        }
    }

    public TypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    // Service implementation for TypeManager interface

    public String[] getSuperTypes(String typeName) {
        try {
            SchemaManager schemaMgr = Framework.getService(SchemaManager.class);
            DocumentType type = schemaMgr.getDocumentType(typeName);
            if (type == null) {
                return null;
            }
            type = (DocumentType) type.getSuperType();
            List<String> superTypes = new ArrayList<String>();
            while (type != null) {
                superTypes.add(type.getName());
                type = (DocumentType) type.getSuperType();
            }
            return superTypes.toArray(new String[superTypes.size()]);
        } catch (Exception e) {
            log.error("Failed to lookup the SchemaManager service", e);
            return new String[0];
        }
    }

    public Type getType(String typeName) {
        return typeRegistry.getType(typeName);
    }

    public boolean hasType(String typeName) {
        return typeRegistry.hasType(typeName);
    }

    public Collection<Type> getTypes() {
        Collection<Type> types = new ArrayList<Type>();
        types.addAll(typeRegistry.getTypes());
        return types;
    }

    public Collection<Type> getAllowedSubTypes(String typeName) {
        return getAllowedSubTypes(typeName, null);
    }

    public Collection<Type> getAllowedSubTypes(String typeName,
            DocumentModel currentDoc) {
        Collection<Type> allowed = new ArrayList<Type>();
        Type type = getType(typeName);
        if (type != null) {
            Map<String, SubType> allowedSubTypes = type.getAllowedSubTypes();
            if (currentDoc != null) {
                allowedSubTypes = filterSubTypesFromConfiguration(
                        allowedSubTypes, currentDoc);
            }
            for (String subTypeName : allowedSubTypes.keySet()) {
                Type subType = getType(subTypeName);
                if (subType != null) {
                    allowed.add(subType);
                }
            }
        }
        return allowed;
    }

    @Override
    public Collection<Type> findAllAllowedSubTypesFrom(String typeName) {
        return findAllAllowedSubTypesFrom(typeName, null, null);
    }

    @Override
    public Collection<Type> findAllAllowedSubTypesFrom(String typeName,
            DocumentModel currentDoc) {
        return findAllAllowedSubTypesFrom(typeName, currentDoc, null);
    }

    protected Collection<Type> findAllAllowedSubTypesFrom(String typeName,
            DocumentModel currentDoc, List<String> alreadyProcessedTypes) {
        if (alreadyProcessedTypes == null) {
            alreadyProcessedTypes = new ArrayList<String>();
        }
        Set<Type> allAllowedSubTypes = new HashSet<Type>();

        Collection<Type> allowedSubTypes = getAllowedSubTypes(typeName,
                currentDoc);
        allAllowedSubTypes.addAll(allowedSubTypes);
        alreadyProcessedTypes.add(typeName);
        for (Type subType : allowedSubTypes) {
            if (!alreadyProcessedTypes.contains(subType.getId())) {
                allAllowedSubTypes.addAll(findAllAllowedSubTypesFrom(
                        subType.getId(), currentDoc, alreadyProcessedTypes));
            }
        }

        return allAllowedSubTypes;
    }

    protected UITypesConfiguration getConfiguration(DocumentModel currentDoc) {
        UITypesConfiguration configuration = null;
        try {
            LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
            configuration = localConfigurationService.getConfiguration(
                    UITypesConfiguration.class, UI_TYPES_CONFIGURATION_FACET,
                    currentDoc);
        } catch (Exception e) {
            log.error(e, e);
        }
        return configuration;
    }

    public Map<String, List<Type>> getTypeMapForDocumentType(String typeName,
            DocumentModel currentDoc) {
        Type type = getType(typeName);
        if (type != null) {
            Map<String, List<Type>> docTypesMap = new HashMap<String, List<Type>>();
            Map<String, SubType> allowedSubTypes = type.getAllowedSubTypes();
            allowedSubTypes = filterSubTypesFromConfiguration(allowedSubTypes,
                    currentDoc);
            for (Map.Entry<String, SubType> entry : allowedSubTypes.entrySet()) {
                if (canCreate(entry.getValue())) {
                    Type subType = getType(entry.getKey());
                    if (subType != null) {
                        String key = subType.getCategory();
                        if (key == null) {
                            key = DEFAULT_CATEGORY;
                        }
                        if (!docTypesMap.containsKey(key)) {
                            docTypesMap.put(key, new ArrayList<Type>());
                        }
                        docTypesMap.get(key).add(subType);
                    }
                }
            }
            return docTypesMap;
        }
        return new HashMap<String, List<Type>>();
    }

    @Override
    public boolean canCreate(String typeName, String containerTypeName) {
        Type containerType = getType(containerTypeName);
        Map<String, SubType> allowedSubTypes = containerType.getAllowedSubTypes();
        return canCreate(typeName, allowedSubTypes);
    }

    @Override
    public boolean canCreate(String typeName, String containerTypeName,
            DocumentModel currentDoc) {
        Map<String, SubType> allowedSubTypes = getFilteredAllowedSubTypes(
                containerTypeName, currentDoc);
        return canCreate(typeName, allowedSubTypes);
    }

    protected Map<String, SubType> getFilteredAllowedSubTypes(
            String containerTypeName, DocumentModel currentDoc) {
        Type containerType = getType(containerTypeName);
        Map<String, SubType> allowedSubTypes = containerType.getAllowedSubTypes();
        return filterSubTypesFromConfiguration(allowedSubTypes, currentDoc);
    }

    protected boolean canCreate(String typeName,
            Map<String, SubType> allowedSubTypes) {
        if (!isAllowedSubType(typeName, allowedSubTypes)) {
            return false;
        }

        SubType subType = allowedSubTypes.get(typeName);
        return canCreate(subType);
    }

    protected boolean canCreate(SubType subType) {
        List<String> hidden = subType.getHidden();
        return !(hidden != null && hidden.contains(HIDDEN_IN_CREATION));
    }

    @Override
    public boolean isAllowedSubType(String typeName, String containerTypeName) {
        Type containerType = getType(containerTypeName);
        Map<String, SubType> allowedSubTypes = containerType.getAllowedSubTypes();
        return isAllowedSubType(typeName, allowedSubTypes);
    }

    protected boolean isAllowedSubType(String typeName,
            Map<String, SubType> allowedSubTypes) {
        for (String subTypeName : allowedSubTypes.keySet()) {
            if (subTypeName.equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAllowedSubType(String typeName, String containerTypeName,
            DocumentModel currentDoc) {
        Map<String, SubType> allowedSubTypes = getFilteredAllowedSubTypes(
                containerTypeName, currentDoc);
        return isAllowedSubType(typeName, allowedSubTypes);
    }

    protected Map<String, SubType> filterSubTypesFromConfiguration(
            Map<String, SubType> allowedSubTypes, DocumentModel currentDoc) {
        UITypesConfiguration uiTypesConfiguration = getConfiguration(currentDoc);
        if (uiTypesConfiguration != null) {
            allowedSubTypes = uiTypesConfiguration.filterSubTypes(allowedSubTypes);
        }
        return allowedSubTypes;
    }

}

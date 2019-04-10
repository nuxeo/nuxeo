/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Contributors:
 *   Jens Huebel, Open Text
 *   Florent Guillaume, Nuxeo
 */
package org.nuxeo.ecm.core.opencmis.impl.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.impl.Converter;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.server.support.TypeManager;

/**
 * Manages a type system for a repository.
 * <p>
 * Types can be added, the inheritance can be managed and type can be retrieved
 * for a given type id.
 * <p>
 * Structures are not copied when returned.
 */
public class TypeManagerImpl implements TypeManager {

    protected Map<String, TypeDefinitionContainer> typesMap = new HashMap<String, TypeDefinitionContainer>();

    @Override
    public TypeDefinitionContainer getTypeById(String typeId) {
        return typesMap.get(typeId);
    }

    @Override
    public TypeDefinition getTypeByQueryName(String typeQueryName) {
        for (Entry<String, TypeDefinitionContainer> entry : typesMap.entrySet()) {
            TypeDefinition type = entry.getValue().getTypeDefinition();
            if (type.getQueryName().equals(typeQueryName))
                return type;
        }
        return null;
    }

    public TypeDefinitionList getTypeChildren(String typeId,
            Boolean includePropertyDefinitions, BigInteger maxItems,
            BigInteger skipCount) {
        // TODO maxItems, skipCount
        TypeDefinitionContainer typec;
        if (typeId == null) {
            // return root types
            typec = null;
        } else {
            typec = typesMap.get(typeId);
            if (typec == null) {
                throw new RuntimeException("No such type: " + typeId);
            }
        }
        List<TypeDefinitionContainer> types;
        if (typec == null) {
            // return root types
            // TODO maintain pre-computed root types
            types = new ArrayList<TypeDefinitionContainer>(4);
            for (TypeDefinitionContainer tc : typesMap.values()) {
                if (tc.getTypeDefinition().getParentTypeId() == null)
                    types.add(tc);
            }
        } else {
            types = typec.getChildren();
        }
        List<TypeDefinition> list = new ArrayList<TypeDefinition>(types.size());
        for (TypeDefinitionContainer tdc : types) {
            TypeDefinition type = tdc.getTypeDefinition();
            if (!Boolean.TRUE.equals(includePropertyDefinitions)) {
                type = Converter.convert(Converter.convert(type)); // clone
                // TODO avoid recomputing type-without-properties
                type.getPropertyDefinitions().clear();
            }
            list.add(type);
        }
        return new TypeDefinitionListImpl(list);
    }

    @Override
    public Collection<TypeDefinitionContainer> getTypeDefinitionList() {
        List<TypeDefinitionContainer> typeRoots = new ArrayList<TypeDefinitionContainer>();
        // iterate types map and return a list collecting the root types:
        for (TypeDefinitionContainer typeCont : typesMap.values()) {
            if (typeCont.getTypeDefinition().getParentTypeId() == null)
                typeRoots.add(typeCont);
        }
        return typeRoots;
    }

    @Override
    public List<TypeDefinitionContainer> getRootTypes() {
        List<TypeDefinitionContainer> rootTypes = new ArrayList<TypeDefinitionContainer>();
        for (TypeDefinitionContainer type : typesMap.values()) {
            String id = type.getTypeDefinition().getId();
            if (BaseTypeId.CMIS_DOCUMENT.value().equals(id)
                    || BaseTypeId.CMIS_FOLDER.value().equals(id)
                    || BaseTypeId.CMIS_RELATIONSHIP.value().equals(id)
                    || BaseTypeId.CMIS_POLICY.value().equals(id)) {
                rootTypes.add(type);
            }
        }
        return rootTypes;
    }

    /**
     * Add a type to the type system. Add all properties from inherited types,
     * add type to children of parent types.
     *
     * @param type new type to add
     */
    public void addTypeDefinition(TypeDefinition type) {
        String id = type.getId();
        if (typesMap.containsKey(id)) {
            throw new RuntimeException("Type already exists: " + id);
        }

        TypeDefinitionContainer typeContainer = new TypeDefinitionContainerImpl(
                type);
        // add type to type map
        typesMap.put(id, typeContainer);

        String parentId = type.getParentTypeId();
        if (parentId != null) {
            if (!typesMap.containsKey(parentId)) {
                throw new RuntimeException("Cannot add type " + id
                        + ", parent does not exist: " + parentId);
            }
            TypeDefinitionContainer parentTypeContainer = typesMap.get(parentId);
            // add new type to children of parent types
            parentTypeContainer.getChildren().add(typeContainer);
            // recursively add inherited properties
            Map<String, PropertyDefinition<?>> propDefs = typeContainer.getTypeDefinition().getPropertyDefinitions();
            addInheritedProperties(propDefs,
                    parentTypeContainer.getTypeDefinition());
        }
    }

    @Override
    public String getPropertyIdForQueryName(TypeDefinition typeDefinition,
            String propQueryName) {
        for (PropertyDefinition<?> pd : typeDefinition.getPropertyDefinitions().values()) {
            if (pd.getQueryName().equals(propQueryName))
                return pd.getId();
        }
        return null;
    }

    protected void addInheritedProperties(
            Map<String, PropertyDefinition<?>> propDefs, TypeDefinition type) {
        if (type.getPropertyDefinitions() != null) {
            addInheritedPropertyDefinitions(propDefs,
                    type.getPropertyDefinitions());
        }
        TypeDefinitionContainer parentTypeContainer = typesMap.get(type.getParentTypeId());
        if (parentTypeContainer != null) {
            addInheritedProperties(propDefs,
                    parentTypeContainer.getTypeDefinition());
        }
    }

    protected void addInheritedPropertyDefinitions(
            Map<String, PropertyDefinition<?>> propDefs,
            Map<String, PropertyDefinition<?>> superPropDefs) {
        for (PropertyDefinition<?> superPropDef : superPropDefs.values()) {
            PropertyDefinition<?> clone = Converter.convert(Converter.convert(superPropDef));
            ((AbstractPropertyDefinition<?>) clone).setIsInherited(Boolean.TRUE);
            propDefs.put(superPropDef.getId(), clone);
        }
    }

}

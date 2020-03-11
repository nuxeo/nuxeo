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

import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.impl.WSConverter;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.apache.chemistry.opencmis.server.support.TypeManager;

/**
 * Manages a type system for a repository.
 * <p>
 * Types can be added, the inheritance can be managed and type can be retrieved for a given type id.
 * <p>
 * Structures are not copied when returned.
 */
public class TypeManagerImpl implements TypeManager {

    public static final int DEFAULT_MAX_TYPE_CHILDREN = 100;

    protected Map<String, TypeDefinitionContainer> typesMap = new HashMap<>();

    protected Map<String, String> propQueryNameToId = new HashMap<>();

    @Override
    public TypeDefinitionContainer getTypeById(String typeId) {
        return typesMap.get(typeId);
    }

    public TypeDefinition getTypeDefinition(String typeId) {
        TypeDefinitionContainer typec = getTypeById(typeId);
        return typec == null ? null : typec.getTypeDefinition();
    }

    /**
     * Checks if a type is known.
     *
     * @param typeId the type id
     * @return {@code true} if known
     * @since 5.9.3
     */
    public boolean hasType(String typeId) {
        return typesMap.containsKey(typeId);
    }

    @Override
    public TypeDefinition getTypeByQueryName(String typeQueryName) {
        for (Entry<String, TypeDefinitionContainer> entry : typesMap.entrySet()) {
            TypeDefinition type = entry.getValue().getTypeDefinition();
            if (type.getQueryName().equals(typeQueryName)) {
                return type;
            }
        }
        return null;
    }

    public TypeDefinitionList getTypeChildren(String typeId, Boolean includePropertyDefinitions, BigInteger maxItems,
            BigInteger skipCount) {
        TypeDefinitionContainer typec;
        if (typeId == null) {
            // return root types
            typec = null;
        } else {
            typec = typesMap.get(typeId);
            if (typec == null) {
                throw new CmisInvalidArgumentException("No such type: " + typeId);
            }
        }
        List<TypeDefinitionContainer> types;
        if (typec == null) {
            // return root types
            // TODO maintain pre-computed root types
            types = new ArrayList<>(4);
            for (TypeDefinitionContainer tc : typesMap.values()) {
                if (tc.getTypeDefinition().getParentTypeId() == null) {
                    types.add(tc);
                }
            }
        } else {
            types = typec.getChildren();
        }
        List<TypeDefinition> list = new ArrayList<>(types.size());
        for (TypeDefinitionContainer tdc : types) {
            TypeDefinition type = tdc.getTypeDefinition();
            if (!Boolean.TRUE.equals(includePropertyDefinitions)) {
                type = WSConverter.convert(WSConverter.convert(type)); // clone
                // TODO avoid recomputing type-without-properties
                type.getPropertyDefinitions().clear();
            }
            list.add(type);
        }
        list = ListUtils.batchList(list, maxItems, skipCount, DEFAULT_MAX_TYPE_CHILDREN);
        return new TypeDefinitionListImpl(list);
    }

    public List<TypeDefinitionContainer> getTypeDescendants(String typeId, int depth, Boolean includePropertyDefinitions) {
        List<TypeDefinitionContainer> types;
        boolean includeProps = Boolean.TRUE.equals(includePropertyDefinitions);
        if (typeId == null) {
            // return all types, unlimited depth
            types = new ArrayList<>(4);
            for (TypeDefinitionContainer tc : typesMap.values()) {
                if (tc.getTypeDefinition().getParentTypeId() == null) {
                    types.add(tc);
                }
            }
            if (!includeProps) {
                // remove props
                types = cloneTypes(types, -1, false);
            }
        } else {
            TypeDefinitionContainer typec = typesMap.get(typeId);
            if (typec == null) {
                throw new CmisInvalidArgumentException("No such type: " + typeId);
            }
            if (depth == 0 || depth < -1) {
                throw new CmisInvalidArgumentException("Invalid depth: " + depth);
            }
            if (depth == -1) {
                types = typec.getChildren();
                if (!includeProps) {
                    // remove props
                    types = cloneTypes(types, -1, false);
                }
            } else {
                types = typec.getChildren();
                // truncate tree
                types = cloneTypes(types, depth - 1, includeProps);
            }
        }
        return types;
    }

    @Override
    public Collection<TypeDefinitionContainer> getTypeDefinitionList() {
        List<TypeDefinitionContainer> typeRoots = new ArrayList<>();
        // iterate types map and return a list collecting the root types:
        for (TypeDefinitionContainer typeCont : typesMap.values()) {
            if (typeCont.getTypeDefinition().getParentTypeId() == null) {
                typeRoots.add(typeCont);
            }
        }
        return typeRoots;
    }

    @Override
    public List<TypeDefinitionContainer> getRootTypes() {
        List<TypeDefinitionContainer> rootTypes = new ArrayList<>();
        for (TypeDefinitionContainer type : typesMap.values()) {
            String id = type.getTypeDefinition().getId();
            if (BaseTypeId.CMIS_DOCUMENT.value().equals(id) || BaseTypeId.CMIS_FOLDER.value().equals(id)
                    || BaseTypeId.CMIS_RELATIONSHIP.value().equals(id) || BaseTypeId.CMIS_POLICY.value().equals(id)) {
                rootTypes.add(type);
            }
        }
        return rootTypes;
    }

    /**
     * Add a type to the type system. Add type to children of parent types. If specified, add all properties from
     * inherited types.,
     *
     * @param type new type to add
     * @param addInheritedProperties
     */
    @Override
    public void addTypeDefinition(TypeDefinition type, boolean addInheritedProperties) {
        String id = type.getId();
        if (typesMap.containsKey(id)) {
            throw new RuntimeException("Type already exists: " + id);
        }

        TypeDefinitionContainer typeContainer = new TypeDefinitionContainerImpl(type);
        // add type to type map
        typesMap.put(id, typeContainer);

        String parentId = type.getParentTypeId();
        if (parentId != null) {
            if (!typesMap.containsKey(parentId)) {
                throw new RuntimeException("Cannot add type " + id + ", parent does not exist: " + parentId);
            }
            TypeDefinitionContainer parentTypeContainer = typesMap.get(parentId);
            // add new type to children of parent types
            parentTypeContainer.getChildren().add(typeContainer);
            if (addInheritedProperties) {
                // recursively add inherited properties
                Map<String, PropertyDefinition<?>> propDefs = typeContainer.getTypeDefinition().getPropertyDefinitions();
                addInheritedProperties(propDefs, parentTypeContainer.getTypeDefinition());
            }
        }

        // prop query names
        for (PropertyDefinition<?> pd : type.getPropertyDefinitions().values()) {
            String propQueryName = pd.getQueryName();
            String propId = pd.getId();
            String old = propQueryNameToId.put(propQueryName, propId);
            if (old != null && !old.equals(propId)) {
                throw new RuntimeException("Cannot add type " + id + ", query name " + propQueryName
                        + " already used for property id " + old);
            }
        }
    }

    public void addTypeDefinition(TypeDefinition type) {
        addTypeDefinition(type, true);
    }

    @Override
    public String getPropertyIdForQueryName(TypeDefinition typeDefinition, String propQueryName) {
        for (PropertyDefinition<?> pd : typeDefinition.getPropertyDefinitions().values()) {
            if (pd.getQueryName().equals(propQueryName)) {
                return pd.getId();
            }
        }
        return null;
    }

    public String getPropertyIdForQueryName(String propQueryName) {
        return propQueryNameToId.get(propQueryName);
    }

    protected void addInheritedProperties(Map<String, PropertyDefinition<?>> propDefs, TypeDefinition type) {
        if (type.getPropertyDefinitions() != null) {
            addInheritedPropertyDefinitions(propDefs, type.getPropertyDefinitions());
        }
        TypeDefinitionContainer parentTypeContainer = typesMap.get(type.getParentTypeId());
        if (parentTypeContainer != null) {
            addInheritedProperties(propDefs, parentTypeContainer.getTypeDefinition());
        }
    }

    protected void addInheritedPropertyDefinitions(Map<String, PropertyDefinition<?>> propDefs,
            Map<String, PropertyDefinition<?>> superPropDefs) {
        for (PropertyDefinition<?> superPropDef : superPropDefs.values()) {
            PropertyDefinition<?> clone = WSConverter.convert(WSConverter.convert(superPropDef));
            ((AbstractPropertyDefinition<?>) clone).setIsInherited(Boolean.TRUE);
            propDefs.put(superPropDef.getId(), clone);
        }
    }

    /**
     * Returns a clone of a types tree.
     * <p>
     * Removes properties on the clone if requested, cuts the children of the clone if the depth is exceeded.
     */
    protected static List<TypeDefinitionContainer> cloneTypes(List<TypeDefinitionContainer> types, int depth,
            boolean includePropertyDefinitions) {
        List<TypeDefinitionContainer> res = new ArrayList<>(types.size());
        TypeDefinitionFactory tdFactory = TypeDefinitionFactory.newInstance();
        for (TypeDefinitionContainer tc : types) {
            MutableTypeDefinition td = tdFactory.copy(tc.getTypeDefinition(), includePropertyDefinitions);
            TypeDefinitionContainerImpl clone = new TypeDefinitionContainerImpl(td);
            if (depth != 0) {
                clone.setChildren(cloneTypes(tc.getChildren(), depth - 1, includePropertyDefinitions));
            }
            res.add(clone);
        }
        return res;
    }

    @Override
    public void updateTypeDefinition(TypeDefinition typeDefinition) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void deleteTypeDefinition(String typeId) {
        throw new CmisNotSupportedException();
    }

}

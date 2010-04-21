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
 * Authors:
 *     Florian Mueller
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.api.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.api.TypeDefinition;
import org.apache.chemistry.opencmis.commons.api.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.api.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.Converter;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractTypeDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;

/**
 * Simple Type Manager.
 */
public class SimpleTypeManager {

    private Map<String, TypeDefinitionContainerImpl> types;

    private List<TypeDefinitionContainer> typesList;

    public SimpleTypeManager() {
        types = new LinkedHashMap<String, TypeDefinitionContainerImpl>();
        typesList = new LinkedList<TypeDefinitionContainer>();
    }

    /**
     * Adds a type while inheriting its parent's properties.
     */
    public void addType(TypeDefinition type) {
        if (types.containsKey(type.getId())) {
            throw new CmisInvalidArgumentException("Type already defined: "
                    + type.getId());
        }
        String parentId = type.getParentTypeId();
        if (parentId != null) {
            TypeDefinition parentType = types.get(parentId).getTypeDefinition();
            type = copyTypeDefinition(type);
            // copy inherited property definition
            for (PropertyDefinition<?> pd : parentType.getPropertyDefinitions().values()) {
                pd = Converter.convert(Converter.convert(pd)); // copy
                ((AbstractPropertyDefinition<?>) pd).setIsInherited(Boolean.TRUE);
                ((AbstractTypeDefinition) type).addPropertyDefinition(pd);
            }
        }

        TypeDefinitionContainerImpl tc = new TypeDefinitionContainerImpl();
        tc.setTypeDefinition(type);

        // add to parent
        if (parentId != null) {
            TypeDefinitionContainerImpl ptc = types.get(parentId);
            if (ptc != null) {
                if (ptc.getChildren() == null) {
                    ptc.setChildren(new ArrayList<TypeDefinitionContainer>());
                }
                ptc.getChildren().add(tc);
            }
        }

        types.put(type.getId(), tc);
        typesList.add(tc);
    }

    /**
     * CMIS getTypesChildren.
     */
    public TypeDefinitionList getTypesChildren(String typeId,
            boolean includePropertyDefinitions, BigInteger maxItems,
            BigInteger skipCount) {
        TypeDefinitionListImpl result = new TypeDefinitionListImpl();
        result.setList(new ArrayList<TypeDefinition>());
        result.setHasMoreItems(Boolean.FALSE);
        result.setNumItems(BigInteger.valueOf(0));

        int skip = (skipCount == null ? 0 : skipCount.intValue());
        if (skip < 0) {
            skip = 0;
        }

        int max = (maxItems == null ? Integer.MAX_VALUE : maxItems.intValue());
        if (max < 1) {
            return result;
        }

        if (typeId == null) {
            if (skip < 1) {
                result.getList().add(
                        copyTypeDefinition(types.get(
                                BaseTypeId.CMIS_FOLDER.value()).getTypeDefinition()));
                max--;
            }
            if ((skip < 2) && (max > 0)) {
                result.getList().add(
                        copyTypeDefinition(types.get(
                                BaseTypeId.CMIS_DOCUMENT.value()).getTypeDefinition()));
                max--;
            }

            result.setHasMoreItems((result.getList().size() + skip) < 2);
            result.setNumItems(BigInteger.valueOf(2));
        } else {
            TypeDefinitionContainer tc = types.get(typeId);
            if ((tc == null) || (tc.getChildren() == null)) {
                return result;
            }

            for (TypeDefinitionContainer child : tc.getChildren()) {
                if (skip > 0) {
                    skip--;
                    continue;
                }

                result.getList().add(
                        copyTypeDefinition(child.getTypeDefinition()));

                max--;
                if (max == 0) {
                    break;
                }
            }

            result.setHasMoreItems((result.getList().size() + skip) < tc.getChildren().size());
            result.setNumItems(BigInteger.valueOf(tc.getChildren().size()));
        }

        if (!includePropertyDefinitions) {
            for (TypeDefinition type : result.getList()) {
                type.getPropertyDefinitions().clear();
            }
        }

        return result;
    }

    /**
     * CMIS getTypesDescendants.
     */
    public List<TypeDefinitionContainer> getTypesDescendants(String typeId,
            BigInteger depth, Boolean includePropertyDefinitions) {
        List<TypeDefinitionContainer> result = new ArrayList<TypeDefinitionContainer>();

        // check depth
        int d = (depth == null ? -1 : depth.intValue());
        if (d == 0) {
            throw new CmisInvalidArgumentException("Depth must not be 0!");
        }

        // set property definition flag to default value if not set
        boolean ipd = (includePropertyDefinitions == null ? false
                : includePropertyDefinitions.booleanValue());

        if (typeId == null) {
            result.add(getTypesDescendants(d,
                    types.get(BaseTypeId.CMIS_FOLDER.value()), ipd));
            result.add(getTypesDescendants(d,
                    types.get(BaseTypeId.CMIS_DOCUMENT.value()), ipd));
        } else {
            TypeDefinitionContainer tc = types.get(typeId);
            if (tc != null) {
                result.add(getTypesDescendants(d, tc, ipd));
            }
        }

        return result;
    }

    /**
     * Gathers the type descendants tree.
     */
    private TypeDefinitionContainer getTypesDescendants(int depth,
            TypeDefinitionContainer tc, boolean includePropertyDefinitions) {
        TypeDefinitionContainerImpl result = new TypeDefinitionContainerImpl();

        TypeDefinition type = copyTypeDefinition(tc.getTypeDefinition());
        if (!includePropertyDefinitions) {
            type.getPropertyDefinitions().clear();
        }

        result.setTypeDefinition(type);

        if (depth != 0) {
            if (tc.getChildren() != null) {
                result.setChildren(new ArrayList<TypeDefinitionContainer>());
                for (TypeDefinitionContainer tdc : tc.getChildren()) {
                    result.getChildren().add(
                            getTypesDescendants(depth < 0 ? -1 : depth - 1,
                                    tdc, includePropertyDefinitions));
                }
            }
        }

        return result;
    }

    public TypeDefinition getType(String typeId) {
        TypeDefinitionContainer tc = types.get(typeId);
        if (tc == null) {
            return null;
        }
        return tc.getTypeDefinition();
    }

    public TypeDefinition getTypeDefinition(String typeId) {
        TypeDefinitionContainer tc = types.get(typeId);
        if (tc == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId
                    + "' is unknown");
        }
        return copyTypeDefinition(tc.getTypeDefinition());
    }

    private TypeDefinition copyTypeDefinition(TypeDefinition type) {
        return Converter.convert(Converter.convert(type));
    }

}

/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.FulltextDescriptor.FulltextIndexDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Info about the fulltext configuration.
 */
public class FulltextConfiguration {

    private static final Log log = LogFactory.getLog(FulltextConfiguration.class);

    public static final String ROOT_TYPE = "Root";

    public static final String PROP_TYPE_STRING = "string";

    public static final String PROP_TYPE_BLOB = "blob";

    public static final String FULLTEXT_DEFAULT_INDEX = "default";

    /** All index names. */
    public final Set<String> indexNames = new LinkedHashSet<String>();

    /** Indexes holding exactly one field. */
    public final Map<String, String> fieldToIndexName = new HashMap<String, String>();

    /** Indexes containing all simple properties. */
    public final Set<String> indexesAllSimple = new HashSet<String>();

    /** Indexes containing all binaries properties. */
    public final Set<String> indexesAllBinary = new HashSet<String>();

    /** Indexes for each specific simple property path. */
    public final Map<String, Set<String>> indexesByPropPathSimple = new HashMap<String, Set<String>>();

    /** Indexes for each specific binary property path. */
    public final Map<String, Set<String>> indexesByPropPathBinary = new HashMap<String, Set<String>>();

    /** Indexes for each specific simple property path excluded. */
    public final Map<String, Set<String>> indexesByPropPathExcludedSimple = new HashMap<String, Set<String>>();

    /** Indexes for each specific binary property path excluded. */
    public final Map<String, Set<String>> indexesByPropPathExcludedBinary = new HashMap<String, Set<String>>();

    // inverse of above maps
    public final Map<String, Set<String>> propPathsByIndexSimple = new HashMap<String, Set<String>>();

    public final Map<String, Set<String>> propPathsByIndexBinary = new HashMap<String, Set<String>>();

    public final Map<String, Set<String>> propPathsExcludedByIndexSimple = new HashMap<String, Set<String>>();

    public final Map<String, Set<String>> propPathsExcludedByIndexBinary = new HashMap<String, Set<String>>();

    public final Set<String> excludedTypes = new HashSet<String>();

    public final Set<String> includedTypes = new HashSet<String>();

    public FulltextConfiguration(FulltextDescriptor fulltextDescriptor) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);

        List<FulltextIndexDescriptor> descs = fulltextDescriptor.getFulltextIndexes();
        if (descs == null) {
            descs = new ArrayList<FulltextIndexDescriptor>(1);
        }
        if (descs.isEmpty()) {
            descs.add(new FulltextIndexDescriptor());
        }
        for (FulltextIndexDescriptor desc : descs) {
            String name = desc.name == null ? FULLTEXT_DEFAULT_INDEX : desc.name;
            indexNames.add(name);
            if (desc.fields == null) {
                desc.fields = new HashSet<String>();
            }
            if (desc.excludeFields == null) {
                desc.excludeFields = new HashSet<String>();
            }
            if (desc.fields.size() == 1 && desc.excludeFields.isEmpty()) {
                fieldToIndexName.put(desc.fields.iterator().next(), name);
            }

            if (desc.fieldType != null) {
                if (desc.fieldType.equals(FulltextConfiguration.PROP_TYPE_STRING)) {
                    indexesAllSimple.add(name);
                } else if (desc.fieldType.equals(FulltextConfiguration.PROP_TYPE_BLOB)) {
                    indexesAllBinary.add(name);
                } else {
                    log.error("Ignoring unknow repository fulltext configuration fieldType: " + desc.fieldType);
                }

            }
            if (desc.fields.isEmpty() && desc.fieldType == null) {
                // no fields specified and no field type -> all of them
                indexesAllSimple.add(name);
                indexesAllBinary.add(name);
            }

            if (fulltextDescriptor.getFulltextExcludedTypes() != null) {
                excludedTypes.addAll(fulltextDescriptor.getFulltextExcludedTypes());
            }
            if (fulltextDescriptor.getFulltextIncludedTypes() != null) {
                includedTypes.addAll(fulltextDescriptor.getFulltextIncludedTypes());
            }

            for (Set<String> fields : Arrays.asList(desc.fields, desc.excludeFields)) {
                for (String path : fields) {
                    boolean include = fields == desc.fields;
                    Field field = schemaManager.getField(path);
                    if (field == null) {
                        log.error(String.format("Ignoring unknown property '%s' in fulltext configuration: %s", path,
                                name));
                        continue;
                    }
                    Type baseType = getBaseType(field.getType());
                    Map<String, Set<String>> indexesByPropPath;
                    Map<String, Set<String>> propPathsByIndex;
                    if (baseType instanceof StringType) {
                        indexesByPropPath = include ? indexesByPropPathSimple : indexesByPropPathExcludedSimple;
                        propPathsByIndex = include ? propPathsByIndexSimple : propPathsExcludedByIndexSimple;
                    } else if (baseType instanceof BinaryType) {
                        indexesByPropPath = include ? indexesByPropPathBinary : indexesByPropPathExcludedBinary;
                        propPathsByIndex = include ? propPathsByIndexBinary : propPathsExcludedByIndexBinary;
                    } else {
                        log.error(String.format("Ignoring property '%s' with bad type %s in fulltext configuration: %s",
                                path, field.getType(), name));
                        continue;
                    }
                    Set<String> indexes = indexesByPropPath.get(path);
                    if (indexes == null) {
                        indexesByPropPath.put(path, indexes = new HashSet<String>());
                    }
                    indexes.add(name);
                    Set<String> paths = propPathsByIndex.get(name);
                    if (paths == null) {
                        propPathsByIndex.put(name, paths = new LinkedHashSet<String>());
                    }
                    paths.add(path);
                }
            }
        }

        // Add document types with the NotFulltextIndexable facet
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            if (documentType.hasFacet(FacetNames.NOT_FULLTEXT_INDEXABLE)) {
                excludedTypes.add(documentType.getName());
            }
        }
    }

    protected Type getBaseType(Type type) {
        if (type instanceof SimpleTypeImpl) {
            return getBaseType(type.getSuperType());
        }
        if (type instanceof ListType) {
            return getBaseType(((ListType) type).getFieldType());
        }
        return type;
    }

    public boolean isFulltextIndexable(String typeName) {
        if (ROOT_TYPE.equals(typeName)) {
            return false;
        }
        if (includedTypes.contains(typeName) || (includedTypes.isEmpty() && !excludedTypes.contains(typeName))) {
            return true;
        }
        return false;
    }

}

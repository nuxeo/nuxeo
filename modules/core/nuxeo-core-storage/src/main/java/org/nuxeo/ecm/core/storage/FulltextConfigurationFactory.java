/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.FulltextDescriptor.FulltextIndexDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Factory building a {@link FulltextConfiguration} from a {@link FulltextDescriptor}.
 *
 * @since 10.3
 */
public class FulltextConfigurationFactory {

    private static final Log log = LogFactory.getLog(FulltextConfigurationFactory.class);

    public static final String PROP_TYPE_STRING = "string";

    public static final String PROP_TYPE_BLOB = "blob";

    public static final String FULLTEXT_DEFAULT_INDEX = "default";

    public static FulltextConfiguration make(FulltextDescriptor fulltextDescriptor) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        FulltextConfiguration ftc = new FulltextConfiguration();
        ftc.fulltextFieldSizeLimit = fulltextDescriptor.getFulltextFieldSizeLimit();
        ftc.fulltextSearchDisabled = fulltextDescriptor.getFulltextSearchDisabled();

        // find what paths we mean by "all"
        // for schemas without prefix, we add both the unprefixed and the prefixed version
        Set<String> allSimplePaths = new HashSet<>();
        Set<String> allBinaryPaths = new HashSet<>();
        PathsFinder pathsFinder = new PathsFinder(allSimplePaths, allBinaryPaths);
        for (Schema schema : schemaManager.getSchemas()) {
            pathsFinder.walkSchema(schema);
        }

        List<FulltextIndexDescriptor> descs = fulltextDescriptor.getFulltextIndexes();
        if (descs == null) {
            descs = new ArrayList<>(1);
        }
        if (descs.isEmpty()) {
            descs.add(new FulltextIndexDescriptor());
        }
        for (FulltextIndexDescriptor desc : descs) {
            String name = desc.name == null ? FULLTEXT_DEFAULT_INDEX : desc.name;
            ftc.indexNames.add(name);
            if (desc.fields == null) {
                desc.fields = new HashSet<>();
            }
            if (desc.excludeFields == null) {
                desc.excludeFields = new HashSet<>();
            }
            if (desc.fields.size() == 1 && desc.excludeFields.isEmpty()) {
                ftc.fieldToIndexName.put(desc.fields.iterator().next(), name);
            }

            if (desc.fieldType != null) {
                if (desc.fieldType.equals(FulltextConfigurationFactory.PROP_TYPE_STRING)) {
                    ftc.indexesAllSimple.add(name);
                } else if (desc.fieldType.equals(FulltextConfigurationFactory.PROP_TYPE_BLOB)) {
                    ftc.indexesAllBinary.add(name);
                } else {
                    log.error("Ignoring unknow repository fulltext configuration fieldType: " + desc.fieldType);
                }

            }
            if (desc.fields.isEmpty() && desc.fieldType == null) {
                // no fields specified and no field type -> all of them
                ftc.indexesAllSimple.add(name);
                ftc.indexesAllBinary.add(name);
            }

            if (ftc.indexesAllSimple.contains(name)) {
                ftc.propPathsByIndexSimple.put(name, new HashSet<>(allSimplePaths));
                for (String path : allSimplePaths) {
                    ftc.indexesByPropPathSimple.computeIfAbsent(path, p -> new HashSet<>()).add(name);
                }
            }
            if (ftc.indexesAllBinary.contains(name)) {
                ftc.propPathsByIndexBinary.put(name, new HashSet<>(allBinaryPaths));
                for (String path : allBinaryPaths) {
                    ftc.indexesByPropPathBinary.computeIfAbsent(path, p -> new HashSet<>()).add(name);
                }
            }

            if (fulltextDescriptor.getFulltextExcludedTypes() != null) {
                ftc.excludedTypes.addAll(fulltextDescriptor.getFulltextExcludedTypes());
            }
            if (fulltextDescriptor.getFulltextIncludedTypes() != null) {
                ftc.includedTypes.addAll(fulltextDescriptor.getFulltextIncludedTypes());
            }

            for (Set<String> fields : Arrays.asList(desc.fields, desc.excludeFields)) {
                boolean include = fields == desc.fields;
                for (String path : fields) {
                    Field field = schemaManager.getField(path);
                    if (field == null && !path.contains(":")) {
                        // check without prefix
                        // TODO precompute this in SchemaManagerImpl
                        int slash = path.indexOf('/');
                        String first = slash == -1 ? path : path.substring(0, slash);
                        for (Schema schema : schemaManager.getSchemas()) {
                            if (!schema.getNamespace().hasPrefix()) {
                                // schema without prefix, try it
                                if (schema.getField(first) != null) {
                                    path = schema.getName() + ":" + path;
                                    field = schemaManager.getField(path);
                                    break;
                                }
                            }
                        }
                    }
                    if (field == null) {
                        log.error(String.format("Ignoring unknown property '%s' in fulltext configuration: %s", path,
                                name));
                        continue;
                    }
                    Type baseType = getBaseType(field.getType());
                    Map<String, Set<String>> indexesByPropPath;
                    Map<String, Set<String>> propPathsByIndex;
                    if (baseType instanceof ComplexType && TypeConstants.isContentType(baseType)) {
                        baseType = ((ComplexType) baseType).getField(BaseDocument.BLOB_DATA).getType(); // BinaryType
                    }
                    if (baseType instanceof StringType) {
                        indexesByPropPath = include ? ftc.indexesByPropPathSimple : ftc.indexesByPropPathExcludedSimple;
                        propPathsByIndex = include ? ftc.propPathsByIndexSimple : ftc.propPathsExcludedByIndexSimple;
                    } else if (baseType instanceof BinaryType) {
                        indexesByPropPath = include ? ftc.indexesByPropPathBinary : ftc.indexesByPropPathExcludedBinary;
                        propPathsByIndex = include ? ftc.propPathsByIndexBinary : ftc.propPathsExcludedByIndexBinary;
                        if (!path.endsWith("/" + BaseDocument.BLOB_DATA)) {
                            path += "/" + BaseDocument.BLOB_DATA;
                            // needed for indexesByPropPathBinary as DBSTransactionState.findDirtyDocuments expects this
                            // to be in the same format as what DirtyPathsFinder expects, like "content/data".
                        }
                    } else {
                        log.error(String.format("Ignoring property '%s' with bad type %s in fulltext configuration: %s",
                                path, field.getType(), name));
                        continue;
                    }
                    indexesByPropPath.computeIfAbsent(path, p -> new HashSet<>()).add(name);
                    propPathsByIndex.computeIfAbsent(name, n -> new HashSet<>()).add(path);
                }
            }
        }

        // Add document types with the NotFulltextIndexable facet
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            if (documentType.hasFacet(FacetNames.NOT_FULLTEXT_INDEXABLE)) {
                ftc.excludedTypes.add(documentType.getName());
            }
        }

        return ftc;
    }

    protected static Type getBaseType(Type type) {
        if (type instanceof SimpleTypeImpl) {
            return getBaseType(type.getSuperType());
        }
        if (type instanceof ListType) {
            return getBaseType(((ListType) type).getFieldType());
        }
        return type;
    }

    /**
     * Accumulates paths for string and binary properties in schemas passed to {@link #walkSchema}.
     * <p>
     * For schemas without prefix the path is accumulated both with and without prefix.
     * <p>
     * For binaries the path includes the final "/data" part.
     */
    // TODO precompute this in SchemaManagerImpl
    public static class PathsFinder {

        protected final Set<String> simplePaths;

        protected final Set<String> binaryPaths;

        public PathsFinder(Set<String> simplePaths, Set<String> binaryPaths) {
            this.simplePaths = simplePaths;
            this.binaryPaths = binaryPaths;
        }

        public void walkSchema(Schema schema) {
            String addPrefix = schema.getNamespace().hasPrefix() ? null : schema.getName();
            walkComplexType(schema, null, addPrefix);
        }

        protected void walkComplexType(ComplexType complexType, String path, String addPrefix) {
            for (Field field : complexType.getFields()) {
                String name = field.getName().getPrefixedName();
                String fieldPath = path == null ? name : path + '/' + name;
                walkType(field.getType(), fieldPath, addPrefix);
            }
        }

        protected void walkType(Type type, String path, String addPrefix) {
            if (type.isSimpleType()) {
                walkSimpleType(type, path, addPrefix);
            } else if (type.isListType()) {
                String listPath = path + "/*";
                Type ftype = ((ListType) type).getField().getType();
                if (ftype.isComplexType()) {
                    // complex list
                    walkComplexType((ComplexType) ftype, listPath, addPrefix);
                } else {
                    // array
                    walkSimpleType(ftype, listPath, addPrefix);
                }
            } else {
                // complex type
                ComplexType ctype = (ComplexType) type;
                walkComplexType(ctype, path, addPrefix);
            }
        }

        protected void walkSimpleType(Type type, String path, String addPrefix) {
            while (type instanceof SimpleTypeImpl) {
                // type with constraint
                type = type.getSuperType();
            }
            if (type instanceof StringType) {
                simplePaths.add(path);
                if (addPrefix != null) {
                    simplePaths.add(addPrefix + ":" + path);
                }
            } else if (type instanceof BinaryType) {
                binaryPaths.add(path);
                if (addPrefix != null) {
                    binaryPaths.add(addPrefix + ":" + path);
                }
            }
        }
    }

}

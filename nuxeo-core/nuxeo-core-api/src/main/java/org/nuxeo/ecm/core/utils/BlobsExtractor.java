/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin Jalon
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Extractor for all the blobs of a document.
 */
public class BlobsExtractor {

    /**
     * Framework boolean property name to fall back on legacy behavior. If true, only blobs referenced by static schemas
     * (attached to the doc type of a document) will be listed i.e. blobs added through dynamic facets will be ignored.
     *
     * @since 2021.37
     */
    public static final String LIST_ONLY_DOC_TYPE_BLOB_PROPERTY_NAME = "nuxeo.document.blob.extractor.legacy";

    /**
     * Local cache of blob paths per doc type.
     */
    protected final Map<String, List<String>> docBlobPaths = new ConcurrentHashMap<>();

    /**
     * Local cache of blob paths per schema.
     */
    protected final Map<String, List<String>> docBlobPathsPerSchema = new ConcurrentHashMap<>();

    private Set<String> includedPaths;

    private Set<String> excludedPaths;

    private boolean allBlobs;

    private boolean isDefaultConfiguration = true;

    /**
     * Sets extractor properties, controlling what properties or values are returned by {@link #getBlobsProperties} or
     * {@link #getBlobs}.
     * <p>
     * The properties have to be defined without prefix if there is no prefix in the schema definition. For blob
     * properties, the path must include the {@code /data} part.
     */
    public void setExtractorProperties(Set<String> includedPaths, Set<String> excludedPaths, boolean allBlobs) {
        this.includedPaths = normalizePaths(includedPaths);
        this.excludedPaths = normalizePaths(excludedPaths);
        this.allBlobs = allBlobs;
        isDefaultConfiguration = includedPaths == null && excludedPaths == null && allBlobs;
    }

    protected boolean isInterestingPath(String path) {
        if (isDefaultConfiguration) {
            return true;
        } else if (excludedPaths != null && excludedPaths.contains(path)) {
            return false;
        } else if (includedPaths != null && includedPaths.contains(path)) {
            return true;
        } else if (allBlobs) {
            return true;
        }
        return false;
    }

    /**
     * Removes the "/data" suffix used by FulltextConfiguration.
     * <p>
     * Adds missing schema name as prefix if no prefix ("content" -> "file:content").
     */
    protected Set<String> normalizePaths(Set<String> paths) {
        if (paths == null) {
            return null;
        }
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Set<String> normPaths = new HashSet<>();
        for (String path : paths) {
            // remove "/data" suffix
            if (path.endsWith("/data")) {
                path = path.substring(0, path.length() - "/data".length());
            }
            // add schema if no schema prefix
            if (schemaManager.getField(path) == null && !path.contains(":")) {
                // check without prefix
                // TODO precompute this in SchemaManagerImpl
                int slash = path.indexOf('/');
                String first = slash == -1 ? path : path.substring(0, slash);
                for (Schema schema : schemaManager.getSchemas()) {
                    if (!schema.getNamespace().hasPrefix()) {
                        // schema without prefix, try it
                        if (schema.getField(first) != null) {
                            path = schema.getName() + ":" + path;
                            break;
                        }
                    }
                }
            }
            normPaths.add(path);
        }
        return normPaths;
    }

    /**
     * Gets the blobs of the document.
     *
     * @param doc the document
     * @return the list of blobs
     */
    public List<Blob> getBlobs(DocumentModel doc) {
        List<Blob> blobs = new ArrayList<>();
        for (Property property : getBlobsProperties(doc)) {
            blobs.add((Blob) property.getValue());
        }
        return blobs;
    }

    /**
     * Gets the blob properties of the document.
     *
     * @param doc the document
     * @return the list of blob properties
     */
    public List<Property> getBlobsProperties(DocumentModel doc) {
        List<String> paths;
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        if (Framework.isBooleanPropertyTrue(LIST_ONLY_DOC_TYPE_BLOB_PROPERTY_NAME)) {
            paths = getBlobPaths(doc.getDocumentType());
        } else {
            paths = Arrays.stream(doc.getSchemas())
                          .map(schemaManager::getSchema)
                          .map(this::getBlobPaths)
                          .flatMap(Collection::stream)
                          .collect(Collectors.toList());
        }
        List<Property> properties = new ArrayList<>();
        for (String path : paths) {
            if (!isInterestingPath(path)) {
                continue;
            }
            // split on:
            // - "[*]" for list
            // - "/" for complex properties
            List<String> split = Arrays.asList(path.split("/[*]/|/"));
            if (split.isEmpty()) {
                throw new IllegalStateException("Path detected not well-formed: " + path);
            }
            Property property = doc.getProperty(split.get(0));
            List<String> subPath = split.subList(1, split.size());
            findBlobsProperties(property, subPath, properties);
        }
        return properties;
    }

    /**
     * Gets the blob paths of the document type. Extractor properties are ignored.
     *
     * @param documentType the document type
     * @return the list of blob paths
     *
     * @since 8.3
     */
    public List<String> getBlobPaths(DocumentType documentType) {
        String docType = documentType.getName();
        List<String> paths = docBlobPaths.get(docType);
        if (paths == null) {
            paths = new ArrayList<>();
            for (Schema schema : documentType.getSchemas()) {
                findBlobPaths(schema, null, schema, paths);
            }
            docBlobPaths.put(docType, paths);
        }
        return paths;
    }

    /**
     * Gets the blob paths of the document's schemas. Extractor properties are ignored.
     *
     * @param schema the schema
     * @return the list of blob paths
     * @since 2021.32
     */
    public List<String> getBlobPaths(Schema schema) {
        return docBlobPathsPerSchema.computeIfAbsent(schema.getName(), n -> {
            List<String> paths = new ArrayList<>();
            findBlobPaths(schema, null, schema, paths);
            return paths;
        });
    }

    protected void findBlobsProperties(Property property, List<String> split, List<Property> properties) {
        if (split.isEmpty()) {
            if (property.getValue() != null) {
                properties.add(property);
            }
        } else {
            String name = split.get(0);
            List<String> subPath = split.subList(1, split.size());
            if (property.isList()) {
                for (Property childProperty : property.getChildren()) {
                    Property childSubProp = childProperty.get(name);
                    findBlobsProperties(childSubProp, subPath, properties);
                }
            } else { // complex type
                Property childSubProp = property.get(name);
                findBlobsProperties(childSubProp, subPath, properties);
            }
        }
    }

    protected void findBlobPaths(ComplexType complexType, String path, Schema schema, List<String> paths) {
        for (Field field : complexType.getFields()) {
            String fieldPath = field.getName().getPrefixedName();
            if (path == null) {
                // add schema name as prefix if the schema doesn't have a prefix
                if (!schema.getNamespace().hasPrefix()) {
                    fieldPath = schema.getName() + ":" + fieldPath;
                }
            } else {
                fieldPath = path + "/" + fieldPath;
            }
            Type type = field.getType();
            if (type.isSimpleType()) {
                continue; // not binary text
            } else if (type.isListType()) {
                Type fieldType = ((ListType) type).getFieldType();
                if (fieldType.isComplexType()) {
                    findBlobPaths((ComplexType) fieldType, fieldPath + "/*", schema, paths);
                } else {
                    continue; // not binary text
                }
            } else { // complex type
                ComplexType ctype = (ComplexType) type;
                if (TypeConstants.isContentType(type)) {
                    // note this path
                    paths.add(fieldPath);
                } else {
                    findBlobPaths(ctype, fieldPath, schema, paths);
                }
            }
        }
    }
}

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Base implementation for a Document.
 * <p>
 * Knows how to read and write values. It is generic in terms of a base State class from which one can read and write
 * values.
 *
 * @since 7.3
 */
public abstract class BaseDocument<T extends StateAccessor> implements Document {

    public static final String BLOB_NAME = "name";

    public static final String BLOB_MIME_TYPE = "mime-type";

    public static final String BLOB_ENCODING = "encoding";

    public static final String BLOB_DIGEST = "digest";

    public static final String BLOB_LENGTH = "length";

    public static final String BLOB_DATA = "data";

    protected final static Pattern NON_CANONICAL_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+)\\]" // index in brackets
    );

    /**
     * Gets the list of proxy schemas, if this is a proxy.
     *
     * @return the proxy schemas, or {@code null}
     */
    protected abstract List<Schema> getProxySchemas();

    /**
     * Gets a child state.
     *
     * @param state the parent state
     * @param name the child name
     * @param type the child's type
     * @return the child state, never {@code null}
     */
    protected abstract T getChild(T state, String name, Type type) throws PropertyException;

    /**
     * Gets a child state which is a list.
     *
     * @param state the parent state
     * @param name the child name
     * @return the child state, never {@code null}
     */
    protected abstract List<T> getChildAsList(T state, String name) throws PropertyException;

    /**
     * Update a list.
     *
     * @param state the parent state
     * @param name the child name
     * @param values the values
     * @param field the list element type
     */
    protected abstract void updateList(T state, String name, List<Object> values, Field field)
            throws PropertyException;

    protected BlobInfo getBlobInfo(T state) throws PropertyException {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = (String) state.getSingle(BLOB_DATA);
        blobInfo.filename = (String) state.getSingle(BLOB_NAME);
        blobInfo.mimeType = (String) state.getSingle(BLOB_MIME_TYPE);
        blobInfo.encoding = (String) state.getSingle(BLOB_ENCODING);
        blobInfo.digest = (String) state.getSingle(BLOB_DIGEST);
        blobInfo.length = (Long) state.getSingle(BLOB_LENGTH);
        return blobInfo;
    }

    protected void setBlobInfo(T state, BlobInfo blobInfo) throws PropertyException {
        state.setSingle(BLOB_DATA, blobInfo.key);
        state.setSingle(BLOB_NAME, blobInfo.filename);
        state.setSingle(BLOB_MIME_TYPE, blobInfo.mimeType);
        state.setSingle(BLOB_ENCODING, blobInfo.encoding);
        state.setSingle(BLOB_DIGEST, blobInfo.digest);
        state.setSingle(BLOB_LENGTH, blobInfo.length);
    }

    /**
     * Canonicalizes a Nuxeo xpath.
     * <p>
     * Replaces {@code a/foo[123]/b} with {@code a/123/b}
     *
     * @param xpath the xpath
     * @return the canonicalized xpath.
     */
    protected static String canonicalXPath(String xpath) {
        if (xpath.indexOf('[') > 0) {
            xpath = NON_CANONICAL_INDEX.matcher(xpath).replaceAll("$1");
        }
        return xpath;
    }

    protected Object getValueObject(T state, String xpath) throws PropertyException {
        xpath = canonicalXPath(xpath);
        String[] segments = xpath.split("/");

        ComplexType parentType = getType();
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            Field field = parentType.getField(segment);
            if (field == null && i == 0) {
                // check facets
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                for (String facet : getFacets()) {
                    CompositeType facetType = schemaManager.getFacet(facet);
                    field = facetType.getField(segment);
                    if (field != null) {
                        break;
                    }
                }
            }
            if (field == null && i == 0 && getProxySchemas() != null) {
                // check proxy schemas
                for (Schema schema : getProxySchemas()) {
                    field = schema.getField(segment);
                    if (field != null) {
                        break;
                    }
                }
            }
            if (field == null) {
                throw new PropertyNotFoundException(xpath, i == 0 ? null : "Unknown segment: " + segment);
            }
            String name = field.getName().getPrefixedName(); // normalize from segment
            Type type = field.getType();

            // check if we have a complex list index in the next position
            if (i < segments.length - 1 && StringUtils.isNumeric(segments[i + 1])) {
                int index = Integer.parseInt(segments[i + 1]);
                i++;
                if (!type.isListType() || ((ListType) type).getFieldType().isSimpleType()) {
                    throw new PropertyNotFoundException(xpath, "Cannot use index after segment: " + segment);
                }
                List<T> list = getChildAsList(state, name);
                if (index >= list.size()) {
                    throw new PropertyNotFoundException(xpath, "Index out of bounds: " + index);
                }
                // find complex list state
                state = list.get(index);
                parentType = (ComplexType) ((ListType) type).getFieldType();
                if (i == segments.length - 1) {
                    // last segment
                    return getValueComplex(state, parentType);
                } else {
                    // not last segment
                    continue;
                }
            }

            if (i == segments.length - 1) {
                // last segment
                return getValueField(state, field);
            } else {
                // not last segment
                if (type.isSimpleType()) {
                    // scalar
                    throw new PropertyNotFoundException(xpath, "Segment must be last: " + segment);
                } else if (type.isComplexType()) {
                    // complex property
                    state = getChild(state, name, type);
                    parentType = (ComplexType) type;
                } else {
                    // list
                    ListType listType = (ListType) type;
                    if (listType.isArray()) {
                        // array of scalars
                        throw new PropertyNotFoundException(xpath, "Segment must be last: " + segment);
                    } else {
                        // complex list but next segment was not numeric
                        throw new PropertyNotFoundException(xpath, "Missing list index after segment: " + segment);
                    }
                }
            }
        }
        throw new AssertionError("not reached");
    }

    protected Object getValueField(T state, Field field) throws PropertyException {
        Type type = field.getType();
        String name = field.getName().getPrefixedName();
        if (type.isSimpleType()) {
            // scalar
            return state.getSingle(name);
        } else if (type.isComplexType()) {
            // complex property
            T childState = getChild(state, name, type);
            return getValueComplex(childState, (ComplexType) type);
        } else {
            // array or list
            Type fieldType = ((ListType) type).getFieldType();
            if (fieldType.isSimpleType()) {
                // array
                return state.getArray(name);
            } else {
                // complex list
                List<T> childStates = getChildAsList(state, name);
                List<Object> list = new ArrayList<>(childStates.size());
                for (T childState : childStates) {
                    Object value = getValueComplex(childState, (ComplexType) fieldType);
                    list.add(value);
                }
                return list;
            }
        }
    }

    protected Object getValueComplex(T state, ComplexType complexType) throws PropertyException {
        if (TypeConstants.isContentType(complexType)) {
            return getValueBlob(state);
        }
        Map<String, Object> map = new HashMap<>();
        for (Field field : complexType.getFields()) {
            String name = field.getName().getPrefixedName();
            Object value = getValueField(state, field);
            map.put(name, value);
        }
        return map;
    }

    protected Blob getValueBlob(T state) throws PropertyException {
        BlobInfo blobInfo = getBlobInfo(state);
        BlobManager blobManager = Framework.getService(BlobManager.class);
        try {
            return blobManager.readBlob(blobInfo, getRepositoryName());
        } catch (IOException e) {
            throw new PropertyException("Cannot get blob info for: " + blobInfo.key, e);
        }
    }

    protected void setValueObject(T state, String xpath, Object value) throws PropertyException {
        xpath = canonicalXPath(xpath);
        String[] segments = xpath.split("/");

        ComplexType parentType = getType();
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            Field field = parentType.getField(segment);
            if (field == null && i == 0) {
                // check facets
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                for (String facet : getFacets()) {
                    CompositeType facetType = schemaManager.getFacet(facet);
                    field = facetType.getField(segment);
                    if (field != null) {
                        break;
                    }
                }
            }
            if (field == null && i == 0 && getProxySchemas() != null) {
                // check proxy schemas
                for (Schema schema : getProxySchemas()) {
                    field = schema.getField(segment);
                    if (field != null) {
                        break;
                    }
                }
            }
            if (field == null) {
                throw new PropertyNotFoundException(xpath, i == 0 ? null : "Unknown segment: " + segment);
            }
            String name = field.getName().getPrefixedName(); // normalize from segment
            Type type = field.getType();

            // check if we have a complex list index in the next position
            if (i < segments.length - 1 && StringUtils.isNumeric(segments[i + 1])) {
                int index = Integer.parseInt(segments[i + 1]);
                i++;
                if (!type.isListType() || ((ListType) type).getFieldType().isSimpleType()) {
                    throw new PropertyNotFoundException(xpath, "Cannot use index after segment: " + segment);
                }
                List<T> list = getChildAsList(state, name);
                if (index >= list.size()) {
                    throw new PropertyNotFoundException(xpath, "Index out of bounds: " + index);
                }
                // find complex list state
                state = list.get(index);
                field = ((ListType) type).getField();
                if (i == segments.length - 1) {
                    // last segment
                    setValueComplex(state, field, value);
                } else {
                    // not last segment
                    parentType = (ComplexType) field.getType();
                    continue;
                }
            }

            if (i == segments.length - 1) {
                // last segment
                setValueField(state, field, value);
            } else {
                // not last segment
                if (type.isSimpleType()) {
                    // scalar
                    throw new PropertyNotFoundException(xpath, "Segment must be last: " + segment);
                } else if (type.isComplexType()) {
                    // complex property
                    state = getChild(state, name, type);
                    parentType = (ComplexType) type;
                } else {
                    // list
                    ListType listType = (ListType) type;
                    if (listType.isArray()) {
                        // array of scalars
                        throw new PropertyNotFoundException(xpath, "Segment must be last: " + segment);
                    } else {
                        // complex list but next segment was not numeric
                        throw new PropertyNotFoundException(xpath, "Missing list index after segment: " + segment);
                    }
                }
            }
        }
    }

    protected void setValueField(T state, Field field, Object value) throws PropertyException {
        Type type = field.getType();
        String name = field.getName().getPrefixedName(); // normalize from map key
        // TODO we could check for read-only here
        if (type.isSimpleType()) {
            // scalar
            state.setSingle(name, value);
        } else if (type.isComplexType()) {
            // complex property
            T childState = getChild(state, name, type);
            setValueComplex(childState, field, value);
        } else {
            // array or list
            ListType listType = (ListType) type;
            Type fieldType = listType.getFieldType();
            if (fieldType.isSimpleType()) {
                // array
                if (value instanceof List) {
                    value = ((List<?>) value).toArray(new Object[0]);
                }
                state.setArray(name, (Object[]) value);
            } else {
                // complex list
                if (value != null && !(value instanceof List)) {
                    throw new PropertyException(
                            "Expected List value for: " + name + ", got " + value.getClass().getName() + " instead");
                }
                @SuppressWarnings("unchecked")
                List<Object> values = value == null ? Collections.emptyList() : (List<Object>) value;
                updateList(state, name, values, listType.getField());
            }
        }
    }

    // pass field instead of just type for better error messages
    protected void setValueComplex(T state, Field field, Object value) throws PropertyException {
        ComplexType complexType = (ComplexType) field.getType();
        if (TypeConstants.isContentType(complexType)) {
            if (value != null && !(value instanceof Blob)) {
                throw new PropertyException("Expected Blob value for: " + field.getName().getPrefixedName() + ", got "
                        + value.getClass().getName() + " instead");
            }
            setValueBlob(state, (Blob) value);
            return;
        }
        if (value != null && !(value instanceof Map)) {
            throw new PropertyException("Expected Map value for: " + field.getName().getPrefixedName() + ", got "
                    + value.getClass().getName() + " instead");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = value == null ? Collections.emptyMap() : (Map<String, Object>) value;
        Set<String> keys = new HashSet<>(map.keySet());
        for (Field f : complexType.getFields()) {
            String name = f.getName().getPrefixedName();
            keys.remove(name);
            value = map.get(name);
            setValueField(state, f, value);
        }
        if (!keys.isEmpty()) {
            throw new PropertyException(
                    "Unknown key: " + keys.iterator().next() + " for " + field.getName().getPrefixedName());
        }
    }

    protected void setValueBlob(T state, Blob blob) throws PropertyException {
        BlobInfo blobInfo = new BlobInfo();
        if (blob != null) {
            BlobManager blobManager = Framework.getService(BlobManager.class);
            try {
                blobInfo.key = blobManager.writeBlob(blob, this);
            } catch (IOException e) {
                throw new PropertyException("Cannot get blob info for: " + blob, e);
            }
            blobInfo.filename = blob.getFilename();
            blobInfo.mimeType = blob.getMimeType();
            blobInfo.encoding = blob.getEncoding();
            blobInfo.digest = blob.getDigest();
            blobInfo.length = blob.getLength() == -1 ? null : Long.valueOf(blob.getLength());
        }
        setBlobInfo(state, blobInfo);
    }

}

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
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
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
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
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

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String BLOB_NAME = "name";

    public static final String BLOB_MIME_TYPE = "mime-type";

    public static final String BLOB_ENCODING = "encoding";

    public static final String BLOB_DIGEST = "digest";

    public static final String BLOB_LENGTH = "length";

    public static final String BLOB_DATA = "data";

    public static final String DC_PREFIX = "dc:";

    public static final String DC_ISSUED = "dc:issued";

    public static final String RELATED_TEXT_RESOURCES = "relatedtextresources";

    public static final String RELATED_TEXT_ID = "relatedtextid";

    public static final String RELATED_TEXT = "relatedtext";

    public static final String FULLTEXT_JOBID_PROP = "ecm:fulltextJobId";

    public static final String FULLTEXT_SIMPLETEXT_PROP = "ecm:simpleText";

    public static final String FULLTEXT_BINARYTEXT_PROP = "ecm:binaryText";

    public static final String MISC_LIFECYCLE_STATE_PROP = "ecm:lifeCycleState";

    public static final String LOCK_OWNER_PROP = "ecm:lockOwner";

    public static final String LOCK_CREATED_PROP = "ecm:lockCreated";

    public static final Set<String> VERSION_WRITABLE_PROPS = new HashSet<String>(Arrays.asList( //
            FULLTEXT_JOBID_PROP, //
            FULLTEXT_BINARYTEXT_PROP, //
            MISC_LIFECYCLE_STATE_PROP, //
            LOCK_OWNER_PROP, //
            LOCK_CREATED_PROP, //
            DC_ISSUED, //
            RELATED_TEXT_RESOURCES, //
            RELATED_TEXT_ID, //
            RELATED_TEXT //
    ));

    protected final static Pattern NON_CANONICAL_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(\\d+)\\]" // index in brackets
    );

    protected static final Runnable NO_DIRTY = () -> {
    };

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
    protected abstract void updateList(T state, String name, List<Object> values, Field field) throws PropertyException;

    /**
     * Update a list.
     *
     * @param state the parent state
     * @param name the child name
     * @param property the property
     */
    protected abstract void updateList(T state, String name, Property property) throws PropertyException;

    /**
     * Finds the internal name to use to refer to this property.
     */
    protected abstract String internalName(String name);

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

    /** Copies the array with an appropriate class depending on the type. */
    protected static Object[] typedArray(Type type, Object[] array) {
        if (array == null) {
            array = EMPTY_STRING_ARRAY;
        }
        Class<?> klass;
        if (type instanceof StringType) {
            klass = String.class;
        } else if (type instanceof BooleanType) {
            klass = Boolean.class;
        } else if (type instanceof LongType) {
            klass = Long.class;
        } else if (type instanceof DoubleType) {
            klass = Double.class;
        } else if (type instanceof DateType) {
            klass = Calendar.class;
        } else if (type instanceof BinaryType) {
            klass = String.class;
        } else if (type instanceof IntegerType) {
            throw new RuntimeException("Unimplemented primitive type: " + type.getClass().getName());
        } else if (type instanceof SimpleTypeImpl) {
            // simple type with constraints -- ignore constraints XXX
            return typedArray(type.getSuperType(), array);
        } else {
            throw new RuntimeException("Invalid primitive type: " + type.getClass().getName());
        }
        int len = array.length;
        Object[] copy = (Object[]) Array.newInstance(klass, len);
        System.arraycopy(array, 0, copy, 0, len);
        return copy;
    }

    protected static boolean isVersionWritableProperty(String name) {
        return VERSION_WRITABLE_PROPS.contains(name) //
                || name.startsWith(FULLTEXT_BINARYTEXT_PROP) //
                || name.startsWith(FULLTEXT_SIMPLETEXT_PROP);
    }

    protected static void clearDirtyFlags(Property property) {
        if (property.isContainer()) {
            for (Property p : property) {
                clearDirtyFlags(p);
            }
        }
        property.clearDirtyFlags();
    }

    /**
     * Checks for ignored writes. May throw.
     */
    protected boolean checkReadOnlyIgnoredWrite(Property property, T state) throws PropertyException {
        String name = property.getField().getName().getPrefixedName();
        if (!isReadOnly() || isVersionWritableProperty(name)) {
            // do write
            return false;
        }
        if (!isVersion()) {
            throw new PropertyException("Cannot write readonly property: " + name);
        }
        if (!name.startsWith(DC_PREFIX)) {
            throw new PropertyException("Cannot set property on a version: " + name);
        }
        // ignore if value is unchanged (only for dublincore)
        // dublincore contains only scalars and arrays
        Object value = property.getValueForWrite();
        Object oldValue;
        if (property.getType().isSimpleType()) {
            oldValue = state.getSingle(name);
        } else {
            oldValue = state.getArray(name);
        }
        if (!ArrayUtils.isEquals(value, oldValue)) {
            // do write
            return false;
        }
        // ignore attempt to write identical value
        return true;
    }

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
     * Gets a value (may be complex/list) from the document at the given xpath.
     */
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
        name = internalName(name);
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

    /**
     * Sets a value (may be complex/list) into the document at the given xpath.
     */
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
        name = internalName(name);
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

    /**
     * Reads state into a complex property.
     */
    protected void readComplexProperty(T state, ComplexProperty complexProperty) throws PropertyException {
        if (state == null) {
            complexProperty.init(null);
            return;
        }
        if (complexProperty instanceof BlobProperty) {
            Blob blob = getValueBlob(state);
            complexProperty.init((Serializable) blob);
            return;
        }
        for (Property property : complexProperty) {
            String name = property.getField().getName().getPrefixedName();
            name = internalName(name);
            Type type = property.getType();
            if (type.isSimpleType()) {
                // simple property
                Object value = state.getSingle(name);
                if (value instanceof Delta) {
                    value = ((Delta) value).getFullValue();
                }
                property.init((Serializable) value);
            } else if (type.isComplexType()) {
                // complex property
                T childState = getChild(state, name, type);
                readComplexProperty(childState, (ComplexProperty) property);
                ((ComplexProperty) property).removePhantomFlag();
            } else {
                ListType listType = (ListType) type;
                if (listType.getFieldType().isSimpleType()) {
                    // array
                    Object[] array = state.getArray(name);
                    array = typedArray(listType.getFieldType(), array);
                    property.init(array);
                } else {
                    // complex list
                    // get

                    Field listField = listType.getField();
                    List<T> childStates = getChildAsList(state, name);
                    // TODO property.init(null) if null children in DBS
                    List<Object> list = new ArrayList<>(childStates.size());
                    for (T childState : childStates) {
                        ComplexProperty p = (ComplexProperty) complexProperty.getRoot().createProperty(property,
                                listField, 0);
                        readComplexProperty(childState, p);
                        list.add(p.getValue());
                    }
                    property.init((Serializable) list);
                }
            }
        }
    }

    /**
     * Writes state from a complex property.
     */
    protected void writeComplexProperty(T state, ComplexProperty complexProperty) throws PropertyException {
        if (complexProperty instanceof BlobProperty) {
            Serializable value = ((BlobProperty) complexProperty).getValueForWrite();
            if (value != null && !(value instanceof Blob)) {
                throw new PropertyException("Cannot write a non-Blob value: " + value);
            }
            setValueBlob(state, (Blob) value);
            return;
        }
        for (Property property : complexProperty) {
            String name = property.getField().getName().getPrefixedName();
            name = internalName(name);
            if (checkReadOnlyIgnoredWrite(property, state)) {
                continue;
            }
            Type type = property.getType();
            if (type.isSimpleType()) {
                // simple property
                Serializable value = property.getValueForWrite();
                state.setSingle(name, value);
                if (value instanceof Delta) {
                    value = ((Delta) value).getFullValue();
                    ((ScalarProperty) property).internalSetValue(value);
                }
                // TODO VersionNotModifiableException
            } else if (type.isComplexType()) {
                // complex property
                T childState = getChild(state, name, type);
                writeComplexProperty(childState, (ComplexProperty) property);
            } else {
                ListType listType = (ListType) type;
                if (listType.getFieldType().isSimpleType()) {
                    // array
                    Serializable value = property.getValueForWrite();
                    if (value instanceof List) {
                        value = ((List<?>) value).toArray(new Object[0]);
                    } else if (!(value == null || value instanceof Object[])) {
                        throw new IllegalStateException(value.toString());
                    }
                    state.setArray(name, (Object[]) value);
                } else {
                    // complex list
                    updateList(state, name, property);
                }
            }
        }
    }

    /**
     * Reads prefetched values.
     */
    protected Map<String, Serializable> readPrefetch(T state, ComplexType complexType, Set<String> xpaths)
            throws PropertyException {
        // augment xpaths with all prefixes, to cut short recursive search
        Set<String> prefixes = new HashSet<>();
        for (String xpath : xpaths) {
            for (;;) {
                // add as prefix
                if (!prefixes.add(xpath)) {
                    // already present, we can stop
                    break;
                }
                // loop with its prefix
                int i = xpath.lastIndexOf('/');
                if (i == -1) {
                    break;
                }
                xpath = xpath.substring(0, i);
            }
        }
        Map<String, Serializable> prefetch = new HashMap<String, Serializable>();
        readPrefetch(state, complexType, null, null, prefixes, prefetch);
        return prefetch;
    }

    protected void readPrefetch(T state, ComplexType complexType, String xpathGeneric, String xpath,
            Set<String> prefixes, Map<String, Serializable> prefetch) throws PropertyException {
        if (TypeConstants.isContentType(complexType)) {
            if (!prefixes.contains(xpathGeneric)) {
                return;
            }
            Blob blob = getValueBlob(state);
            prefetch.put(xpath, (Serializable) blob);
            return;
        }
        for (Field field : complexType.getFields()) {
            readPrefetchField(state, field, xpathGeneric, xpath, prefixes, prefetch);
        }
    }

    protected void readPrefetchField(T state, Field field, String xpathGeneric, String xpath, Set<String> prefixes,
            Map<String, Serializable> prefetch) {
        String name = field.getName().getPrefixedName();
        Type type = field.getType();
        xpathGeneric = xpathGeneric == null ? name : xpathGeneric + '/' + name;
        xpath = xpath == null ? name : xpath + '/' + name;
        if (!prefixes.contains(xpathGeneric)) {
            return;
        }
        if (type.isSimpleType()) {
            // scalar
            Object value = state.getSingle(name);
            prefetch.put(xpath, (Serializable) value);
        } else if (type.isComplexType()) {
            // complex property
            T childState = getChild(state, name, type);
            readPrefetch(childState, (ComplexType) type, xpathGeneric, xpath, prefixes, prefetch);
        } else {
            // array or list
            ListType listType = (ListType) type;
            if (listType.getFieldType().isSimpleType()) {
                // array
                Object[] value = state.getArray(name);
                prefetch.put(xpath, value);
            } else {
                // complex list
                List<T> childStates = getChildAsList(state, name);
                Field listField = listType.getField();
                xpathGeneric += "/*";
                int i = 0;
                for (T childState : childStates) {
                    readPrefetch(childState, (ComplexType) listField.getType(), xpathGeneric, xpath + '/' + i++,
                            prefixes, prefetch);
                }
            }
        }
    }

    /**
     * Visits all the blobs of this document and calls the passed blob visitor on each one.
     */
    protected void visitBlobs(T state, Consumer<BlobAccessor> blobVisitor, Runnable markDirty)
            throws PropertyException {
        Visit visit = new Visit(blobVisitor, markDirty);
        // structural type
        visit.visitBlobsComplex(state, getType());
        // dynamic facets
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        for (String facet : getFacets()) {
            CompositeType facetType = schemaManager.getFacet(facet);
            visit.visitBlobsComplex(state, facetType);
        }
        // proxy schemas
        if (getProxySchemas() != null) {
            for (Schema schema : getProxySchemas()) {
                visit.visitBlobsComplex(state, schema);
            }
        }
    }

    protected class StateBlobAccessor implements BlobAccessor {

        protected final Collection<String> path;

        protected final T state;

        protected final Runnable markDirty;

        public StateBlobAccessor(Collection<String> path, T state, Runnable markDirty) {
            this.path = path;
            this.state = state;
            this.markDirty = markDirty;
        }

        @Override
        public String getXPath() {
            return StringUtils.join(path, "/");
        }

        @Override
        public Blob getBlob() throws PropertyException {
            return getValueBlob(state);
        }

        @Override
        public void setBlob(Blob blob) throws PropertyException {
            markDirty.run();
            setValueBlob(state, blob);
        }
    }

    protected class Visit {

        protected final Consumer<BlobAccessor> blobVisitor;

        protected final Runnable markDirty;

        protected final Deque<String> path;

        public Visit(Consumer<BlobAccessor> blobVisitor, Runnable markDirty) {
            this.blobVisitor = blobVisitor;
            this.markDirty = markDirty;
            path = new ArrayDeque<>();
        }

        public void visitBlobsComplex(T state, ComplexType complexType) throws PropertyException {
            if (TypeConstants.isContentType(complexType)) {
                blobVisitor.accept(new StateBlobAccessor(path, state, markDirty));
                return;
            }
            for (Field field : complexType.getFields()) {
                visitBlobsField(state, field);
            }
        }

        protected void visitBlobsField(T state, Field field) throws PropertyException {
            Type type = field.getType();
            if (type.isSimpleType()) {
                // scalar
            } else if (type.isComplexType()) {
                // complex property
                String name = field.getName().getPrefixedName();
                T childState = getChild(state, name, type);
                path.addLast(name);
                visitBlobsComplex(childState, (ComplexType) type);
                path.removeLast();
            } else {
                // array or list
                Type fieldType = ((ListType) type).getFieldType();
                if (fieldType.isSimpleType()) {
                    // array
                } else {
                    // complex list
                    String name = field.getName().getPrefixedName();
                    path.addLast(name);
                    int i = 0;
                    for (T childState : getChildAsList(state, name)) {
                        path.addLast(String.valueOf(i++));
                        visitBlobsComplex(childState, (ComplexType) fieldType);
                        path.removeLast();
                    }
                    path.removeLast();
                }
            }
        }
    }

}

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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.runtime.api.Framework;

/**
 * Property handling an external blob: create/edit is done from a map, and the value returned is a blob.
 * <p>
 * Create/edit from a blob is not handled, and the blob uri cannot be retrieved from the blob (no api for now).
 *
 * @author Anahide Tchertchian
 */
public class ExternalBlobProperty extends MapProperty {

    private static final long serialVersionUID = 1L;

    // constants based on core-types.xsd fields. XXX Should be in model
    public static final String ENCODING = "encoding";

    public static final String MIME_TYPE = "mime-type";

    public static final String FILE_NAME = "name";

    public static final String DIGEST = "digest";

    public static final String LENGTH = "length";

    public static final String URI = "uri";

    public ExternalBlobProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    public ExternalBlobProperty(Property parent, Field field) {
        super(parent, field);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Serializable value) throws PropertyException {
        if (value == null) {
            // IGNORE null values - properties will be
            // considered PHANTOMS
            return;
        }
        Map<String, Serializable> map;
        if (value instanceof Map) {
            map = (Map<String, Serializable>) value;
        } else if (value instanceof Blob) {
            // XXX: workaround: get the uri from the local prop because it's not on
            // the Blob
            map = getMapFromBlobWithUri((Blob) value);
        } else {
            throw new PropertyException("Invalid value for external blob (map or blob needed): " + value);
        }
        for (Entry<String, Serializable> entry : map.entrySet()) {
            Property property = get(entry.getKey());
            property.init(entry.getValue());
        }
        removePhantomFlag();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Serializable internalGetValue() throws PropertyException {
        Object mapValue = super.internalGetValue();
        if (mapValue instanceof Map) {
            Blob blob = getBlobFromMap((Map<String, Object>) mapValue);
            if (blob != null && !(blob instanceof Serializable)) {
                throw new PropertyException("Blob is not serializable: " + blob);
            }
            return (Serializable) blob;
        } else if (mapValue != null) {
            throw new PropertyException("Invalid value for external blob (map needed): " + mapValue);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(Class<T> type) throws PropertyException {
        Object value = super.internalGetValue();
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            if (Map.class.isAssignableFrom(type)) {
                return (T) value;
            }
            if (Blob.class.isAssignableFrom(type)) {
                return (T) getBlobFromMap((Map<String, Object>) value);
            }
        }
        throw new PropertyConversionException(value.getClass(), type);
    }

    @Override
    public Serializable getValueForWrite() throws PropertyException {
        return (Serializable) getValue(Map.class);
    }

    /**
     * Overridden to be able to set a blob from a given map.
     * <p>
     * Take care of not overriding the uri if set as this information is not on the blob.
     *
     * @throws PropertyException if one of the sub properties throws an exception or if trying to set values to a blob
     *             without any already existing uri set.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Object value) throws PropertyException {
        if (!isContainer()) { // if not a container use default setValue()
            super.setValue(value);
            return;
        }
        if (isReadOnly() || isSecured()) {
            throw new ReadOnlyPropertyException(
                    String.format("Cannot set the value of property: %s since it is readonly", getXPath()));
        }
        if (value == null) {
            remove();
            return; // TODO how to treat nulls?
        }
        if (value instanceof Blob) {
            Property property = get(URI);
            Object uri = property.getValue();
            if (uri == null) {
                throw new PropertyException("Cannot set blob properties without " + "an existing uri set");
            }
            // only update additional properties
            Map<String, Serializable> map = getMapFromBlob((Blob) value);
            for (Entry<String, Serializable> entry : map.entrySet()) {
                String entryKey = entry.getKey();
                if (entryKey != URI) {
                    property = get(entryKey);
                    property.setValue(entry.getValue());
                }
            }
            return;
        }
        if (!(value instanceof Map)) {
            throw new InvalidPropertyValueException(getXPath());
        }
        Map<String, Object> map = (Map<String, Object>) value;
        for (Entry<String, Object> entry : map.entrySet()) {
            Property property = get(entry.getKey());
            property.setValue(entry.getValue());
        }
    }

    @Override
    protected boolean isSameValue(Serializable value1, Serializable value2) {
        // for now, blob property are considered always as dirty when update - see NXP-16322
        return false;
    }

    public static Blob getBlobFromMap(Map<String, Object> mapValue) {
        if (mapValue == null) {
            return null;
        }
        String uri = (String) mapValue.get(URI);
        if (uri == null || "".equals(uri)) {
            return null;
        }
        String filename = (String) mapValue.get(FILE_NAME);
        String mimeType = (String) mapValue.get(MIME_TYPE);
        String encoding = (String) mapValue.get(ENCODING);
        String digest = (String) mapValue.get(DIGEST);
        try {
            BlobHolderAdapterService service = Framework.getService(BlobHolderAdapterService.class);
            if (service == null) {
                throw new NuxeoException("BlobHolderAdapterService not found");
            }
            Blob blob = service.getExternalBlobForUri(uri);
            if (filename != null) {
                blob.setFilename(filename);
            }
            blob.setMimeType(mimeType);
            blob.setEncoding(encoding);
            // TODO maybe check if digest is still a match to the retrieved blob
            blob.setDigest(digest);
            return blob;
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    public Map<String, Serializable> getMapFromBlobWithUri(Blob blob) throws PropertyException {
        Map<String, Serializable> map = getMapFromBlob(blob);
        Property property = get(URI);
        Serializable uri = property.getValue();
        map.put(URI, uri);
        return map;
    }

    public static Map<String, Serializable> getMapFromBlob(Blob blob) {
        Map<String, Serializable> map = new HashMap<>();
        if (blob == null) {
            map.put(FILE_NAME, null);
            map.put(MIME_TYPE, null);
            map.put(ENCODING, null);
            map.put(LENGTH, null);
            map.put(DIGEST, null);
        } else {
            // cannot return uri for blob for now: no edit implemented
            map.put(FILE_NAME, blob.getFilename());
            map.put(MIME_TYPE, blob.getMimeType());
            map.put(ENCODING, blob.getEncoding());
            map.put(LENGTH, blob.getLength());
            map.put(DIGEST, blob.getDigest());
        }
        return map;
    }

}

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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * Blob property, reading and writing from a {@link Blob} object.
 */
public class BlobProperty extends MapProperty {

    private static final long serialVersionUID = 1L;

    private static final String NAME = "name";

    private static final String MIME_TYPE = "mime-type";

    private static final String ENCODING = "encoding";

    private static final String DIGEST = "digest";

    private static final String LENGTH = "length";

    protected Serializable value;

    public BlobProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public Serializable getDefaultValue() {
        return null;
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        return value;
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
        this.value = value;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException {
        return (BlobProperty) super.clone();
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || ((value instanceof Blob) && (value instanceof Serializable));
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (isNormalized(value)) {
            // TODO specific blob support?
            return (Serializable) value;
        }
        throw new PropertyConversionException(value.getClass(), Blob.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType) throws PropertyConversionException {
        if (value == null) {
            return null;
        }
        if (Blob.class.isAssignableFrom(toType)) {
            return (T) value;
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() {
        return new ByteArrayInputStream("".getBytes()); // TODO not serializable
    }

    @Override
    public boolean isContainer() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(Object value) throws PropertyException {
        if (value instanceof Map) {
            setMap(getValue(), (Map<String, Object>) value);
            setIsModified();
        } else {
            super.setValue(value);
        }
    }

    @Override
    protected boolean isSameValue(Serializable value1, Serializable value2) {
        // for now, blob property are considered always as dirty when update - see NXP-16322
        return false;
    }

    @Override
    public void init(Serializable value) throws PropertyException {
        if (value == null) {
            // IGNORE null values - properties will be
            // considered PHANTOMS
            return;
        }
        if (value instanceof Blob) {
            internalSetValue(value);
        }
        removePhantomFlag();
    }

    @Override
    public Serializable getValueForWrite() throws PropertyException {
        return getValue();
    }

    @Override
    protected Property internalGetChild(Field field) {
        return new ScalarMemberProperty(this, field, isPhantom() ? IS_PHANTOM : 0);
    }

    protected void setMap(Object object, Map<String, Object> value) throws PropertyException {
        if (object == null) {
            throw new NuxeoException("Trying to access a member of a null object");
        }
        if (!(object instanceof Blob)) {
            throw new NuxeoException("Not a Blob: " + object);
        }
        Blob blob = (Blob) object;
        for (Entry<String, Object> entry : value.entrySet()) {
            String name = entry.getKey();
            Object v = entry.getValue();
            setMemberValue(blob, name, v);
        }
    }

    protected void setMemberValue(Blob blob, String name, Object value) throws PropertyNotFoundException {
        if (NAME.equals(name)) {
            blob.setFilename((String) value);
        } else if (MIME_TYPE.equals(name)) {
            blob.setMimeType((String) value);
        } else if (ENCODING.equals(name)) {
            blob.setEncoding((String) value);
        } else if (DIGEST.equals(name)) {
            blob.setDigest((String) value);
        } else {
            throw new PropertyNotFoundException(name);
        }
    }

    protected Object getMemberValue(Object object, String name) throws PropertyException {
        if (object == null) {
            throw new NuxeoException("Trying to access a member of a null object: " + name);
        }
        if (!(object instanceof Blob)) {
            throw new NuxeoException("Not a Blob: " + object);
        }
        Blob blob = (Blob) object;
        if (NAME.equals(name)) {
            return blob.getFilename();
        } else if (MIME_TYPE.equals(name)) {
            return blob.getMimeType();
        } else if (ENCODING.equals(name)) {
            return blob.getEncoding();
        } else if (DIGEST.equals(name)) {
            return blob.getDigest();
        } else if (LENGTH.equals(name)) {
            return Long.valueOf(blob.getLength());
        } else {
            throw new PropertyNotFoundException(name);
        }
    }

    protected void setMemberValue(Object object, String name, Object value) throws PropertyException {
        if (object == null) {
            throw new NuxeoException("Trying to access a member of a null object: " + name);
        }
        if (!(object instanceof Blob)) {
            throw new NuxeoException("Not a Blob: " + object);
        }
        Blob blob = (Blob) object;
        setMemberValue(blob, name, value);
    }

    public static class ScalarMemberProperty extends ScalarProperty {

        private static final long serialVersionUID = 1L;

        public ScalarMemberProperty(Property parent, Field field, int flags) {
            super(parent, field, flags);
        }

        @Override
        public void internalSetValue(Serializable value) throws PropertyException {
            ((BlobProperty) parent).setMemberValue(parent.getValue(), getName(), value);
        }

        @Override
        public Serializable internalGetValue() throws PropertyException {
            Object value = ((BlobProperty) parent).getMemberValue(parent.getValue(), getName());
            if (value != null && !(value instanceof Serializable)) {
                throw new PropertyException("Non serializable value: " + value);
            }
            return (Serializable) value;
        }
    }

    @Override
    public boolean isSameAs(Property property) throws PropertyException {
        if (!(property instanceof BlobProperty)) {
            return false;
        }
        BlobProperty other = (BlobProperty) property;
        return ObjectUtils.equals(getValue(), other.getValue());
    }

}

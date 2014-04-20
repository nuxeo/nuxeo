/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyAccessException;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.runtime.services.streaming.ByteArraySource;
import org.nuxeo.runtime.services.streaming.InputStreamSource;
import org.nuxeo.runtime.services.streaming.StreamSource;
import org.nuxeo.runtime.services.streaming.StringSource;

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

    private static final String DATA = "data";

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
        return value == null
                || ((value instanceof Blob) && (value instanceof Serializable));
    }

    @Override
    public Serializable normalize(Object value)
            throws PropertyConversionException {
        if (isNormalized(value)) {
            // TODO specific blob support?
            return (Serializable) value;
        }
        throw new PropertyConversionException(value.getClass(), Blob.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException {
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

    @SuppressWarnings("unchecked")
    @Override
    public void init(Serializable value) throws PropertyException {
        if (value == null) {
            // IGNORE null values - properties will be
            // considered PHANTOMS
            return;
        }
        if (value instanceof Map) {
            internalSetValue((Serializable) create((Map<String, Object>) value));
        } else {
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
        return new ScalarMemberProperty(this, field, isPhantom() ? IS_PHANTOM
                : 0);
    }

    @Override
    public boolean isSameAs(Property property) throws PropertyException {
        if (property == null) {
            return false;
        }
        ScalarProperty sp = (ScalarProperty) property;
        Object v1 = getValue();
        Object v2 = sp.getValue();
        if (v1 == null) {
            return v2 == null;
        }
        return v1.equals(v2);
    }

    protected Object create(Map<String, Object> value) {
        Object data = value.get(DATA);
        StreamSource ss;
        if (data instanceof String) {
            ss = new StringSource((String) data);
        } else if (data instanceof InputStream) {
            ss = new InputStreamSource((InputStream) data);
        } else if (data instanceof byte[]) {
            ss = new ByteArraySource((byte[]) data);
        } else {
            ss = new ByteArraySource(new byte[0]);
        }
        Blob blob = new StreamingBlob(ss);
        try {
            Map<String,Object> v = new HashMap<String, Object>(value);
            v.remove(DATA);
            setMap(blob, v);
        } catch (PropertyException e) {
            throw new RuntimeException(e);
        }
        return blob;
    }

    protected void setMap(Object object, Map<String, Object> value)
            throws PropertyException {
        if (object == null) {
            throw new PropertyAccessException(
                    "Trying to access a member of a null object");
        }
        if (!(object instanceof Blob)) {
            throw new PropertyAccessException("Not a Blob: " + object);
        }
        Blob blob = (Blob) object;
        for (Entry<String, Object> entry : value.entrySet()) {
            String name = entry.getKey();
            Object v = entry.getValue();
            setMemberValue(blob, name, v);
        }
    }

    protected void setMemberValue(Blob blob, String name, Object value)
            throws PropertyNotFoundException {
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

    protected Object getMemberValue(Object object, String name)
            throws PropertyException {
        if (object == null) {
            throw new PropertyAccessException(
                    "Trying to access a member of a null object: " + name);
        }
        if (!(object instanceof Blob)) {
            throw new PropertyAccessException("Not a Blob: " + object);
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

    protected void setMemberValue(Object object, String name, Object value)
            throws PropertyException {
        if (object == null) {
            throw new PropertyAccessException(
                    "Trying to access a member of a null object: " + name);
        }
        if (!(object instanceof Blob)) {
            throw new PropertyAccessException("Not a Blob: " + object);
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
        public void internalSetValue(Serializable value)
                throws PropertyException {
            ((BlobProperty) parent).setMemberValue(parent.getValue(),
                    getName(), value);
        }

        @Override
        public Serializable internalGetValue() throws PropertyException {
            Object value = ((BlobProperty) parent).getMemberValue(
                    parent.getValue(), getName());
            if (value != null && !(value instanceof Serializable)) {
                throw new PropertyException("Non serializable value: " + value);
            }
            return (Serializable) value;
        }
    }

}

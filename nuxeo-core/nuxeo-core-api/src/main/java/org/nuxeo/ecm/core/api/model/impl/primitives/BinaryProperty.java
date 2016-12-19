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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BinaryProperty extends ScalarProperty {

    private static final long serialVersionUID = 1L;

    public BinaryProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value instanceof InputStream;
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (isNormalized(value)) {
            // TODO if input stream is not serializable? do we convert to a serializable input stream?
            return (Serializable) value;
        }
        throw new PropertyConversionException(value.getClass(), InputStream.class);
        // TODO byte array is not serializable
        // if (value.getClass() == String.class) {
        // return new ByteArrayInputStream(((String)value).getBytes());
        // }
        // if (value.getClass() == byte[].class) {
        // return new ByteArrayInputStream((byte[])value.);
        // }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType) throws PropertyConversionException {
        if (value == null) {
            return null;
        }
        if (InputStream.class.isAssignableFrom(toType)) {
            return (T) value;
        }
        if (toType == String.class && value instanceof InputStream) {
            try (InputStream in = (InputStream) value){
                return (T) IOUtils.toString(in, Charsets.UTF_8);
            } catch (IOException e) {
                throw new InvalidPropertyValueException("Failed to read given input stream", e);
            }
        }
        if (toType == byte[].class && value instanceof InputStream) {
            try {
                return (T) FileUtils.readBytes((InputStream) value);
            } catch (IOException e) {
                throw new InvalidPropertyValueException("Failed to read given input stream", e);
            }
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() {
        return new ByteArrayInputStream("".getBytes()); // TODO not serializable
    }

}

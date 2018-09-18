/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.io.marshallers.csv;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentPropertyCSVWriter extends AbstractCSVWriter<Property> {

    public DocumentPropertyCSVWriter() {
        super(Property.class);
    }

    @Override
    public void write(Property entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
            throws IOException {
        if (entity.isScalar()) {
            writeScalarProperty(out, entity);
        } else {
            writeUnsupported(out, entity);
        }

    }

    protected void writeScalarProperty(OutputStream out, Property entity) throws IOException {
        Object value = entity.getValue();
        if (value == null) {
            write(out, null);
        } else if (entity.getType() instanceof BinaryType) {
            writeUnsupported(out, entity);
        } else {
            write(out, entity.getType().encode(value));
        }
    }

    protected void writeUnsupported(OutputStream out, Property entity) throws IOException {
        write(out, String.format("type %s is not supported", entity.getType().getName()));
    }

}

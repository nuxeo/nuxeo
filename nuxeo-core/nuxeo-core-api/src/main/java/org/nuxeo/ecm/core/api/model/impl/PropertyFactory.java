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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model.impl;

import static org.nuxeo.ecm.core.api.model.impl.AbstractProperty.IS_SECURED;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BinaryProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BooleanProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.DateProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.DoubleProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.ExternalBlobProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.LongProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;
import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.ecm.core.schema.TypeConstants;
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
 * The default property factory singleton.
 */
public class PropertyFactory {

    private PropertyFactory() {
        // utility class
    }

    public static Property createProperty(Property parent, Field field, int f) {
        int flags = f;
        // check if property to create is a secured one
        if (parent instanceof AbstractProperty) { // should always be the case
            if (((AbstractProperty) parent).areFlagsSet(IS_SECURED)) {
                flags |= IS_SECURED;
            } else if ((flags & IS_SECURED) == 0) {
                Schema schema = parent.getSchema();
                StringBuilder xpath = new StringBuilder();
                if (parent.getParent() != null) {
                    xpath.append(parent.getXPath()).append('/');
                }
                xpath.append(field.getName().getLocalName());

                PropertyCharacteristicHandler propertyHandler = Framework.getService(
                        PropertyCharacteristicHandler.class);
                if (propertyHandler.isSecured(schema.getName(), xpath.toString())) {
                    flags |= IS_SECURED;
                }
            }
        }

        Type type = field.getType();
        if (type instanceof SimpleTypeImpl) {
            // type with constraint
            type = type.getSuperType();
        }
        switch (type.getName()) {
        case StringType.ID:
            return new StringProperty(parent, field, flags);
        case IntegerType.ID:
        case LongType.ID:
            return new LongProperty(parent, field, flags);
        case DoubleType.ID:
            return new DoubleProperty(parent, field, flags);
        case BooleanType.ID:
            return new BooleanProperty(parent, field, flags);
        case DateType.ID:
            return new DateProperty(parent, field, flags);
        case BinaryType.ID:
            return new BinaryProperty(parent, field, flags);
        case TypeConstants.CONTENT:
            return new BlobProperty(parent, field, flags);
        case TypeConstants.EXTERNAL_CONTENT:
            return new ExternalBlobProperty(parent, field, flags);
        }
        if (type.isSimpleType()) {
            return new ScalarProperty(parent, field, flags);
        } else if (type.isComplexType()) {
            return new MapProperty(parent, field, flags);
        } else if (type.isListType()) {
            if (((ListType) type).isArray()) {
                return new ArrayProperty(parent, field, flags);
            } else {
                return new ListProperty(parent, field, flags);
            }
        } else {
            throw new IllegalArgumentException("Unsupported field type: " + field.getType().getName());
        }
    }

}

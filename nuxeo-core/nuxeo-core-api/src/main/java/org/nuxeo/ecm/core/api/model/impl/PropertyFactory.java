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
package org.nuxeo.ecm.core.api.model.impl;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BinaryProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BooleanProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.DateProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.DoubleProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.ExternalBlobProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.LongProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 * The default property factory singleton.
 */
public class PropertyFactory {

    private PropertyFactory() {
        // utility class
    }

    public static Property createProperty(Property parent, Field field, int flags) {
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
            throw new IllegalArgumentException("Unsupported field type: "
                    + field.getType().getName());
        }
    }

}

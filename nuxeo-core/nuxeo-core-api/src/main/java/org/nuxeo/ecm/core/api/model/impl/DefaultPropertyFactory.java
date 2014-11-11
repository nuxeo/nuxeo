/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl;

import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyFactory;
import org.nuxeo.ecm.core.api.model.impl.primitives.BinaryProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BooleanProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.DateProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.DoubleProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.ExternalBlobProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.LongProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
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
 * A composite property factory that uses children factories to create
 * properties.
 * <p>
 * The children factories are registered under a string key that is the type
 * name corresponding to the property that is to be created. The type name can
 * be specified as an absolute or as a local type name. For example if the
 * global type <code>string</code> is redefined by a schema
 * <code>myschema</code> then you need to use the absolute type name to refer
 * to that type: <code>myschema:string</code>.
 * <p>
 * If one looks up a factory using an absolute type name - the absolute name
 * will be used and if no factory is found then the local type name is used.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultPropertyFactory extends CompositePropertyFactory {

    private static DefaultPropertyFactory instance;

    public static final PropertyFactory DEFAULT = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return newProperty(parent, field, flags);
        }
    };

    public static final PropertyFactory STRING = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return new StringProperty(parent, field, flags);
        }
    };

    public static final PropertyFactory INTEGER = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return new LongProperty(parent, field, flags);
        }
    };

    public static final PropertyFactory DOUBLE = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return new DoubleProperty(parent, field, flags);
        }
    };

    public static final PropertyFactory DATE = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return new DateProperty(parent, field, flags);
        }
    };

    public static final PropertyFactory BOOLEAN = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return new BooleanProperty(parent, field, flags);
        }
    };

    public static final PropertyFactory BINARY = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return new BinaryProperty(parent, field, flags);
        }
    };

    public static final PropertyFactory BLOB = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return new BlobProperty(parent, field, flags);
        }
    };

    public static final PropertyFactory EXTERNAL_BLOB = new PropertyFactory() {
        @Override
        public Property createProperty(Property parent, Field field, int flags) {
            return new ExternalBlobProperty(parent, field, flags);
        }
    };

    public static DefaultPropertyFactory getInstance() {
        if (instance == null) {
            instance = new DefaultPropertyFactory();
            instance.registerFactory(StringType.ID, STRING);
            instance.registerFactory(LongType.ID, INTEGER);
            instance.registerFactory(IntegerType.ID, INTEGER);
            instance.registerFactory(DoubleType.ID, DOUBLE);
            instance.registerFactory(BooleanType.ID, BOOLEAN);
            instance.registerFactory(DateType.ID, DATE);
            instance.registerFactory(BinaryType.ID, BINARY);
            instance.registerFactory(TypeConstants.CONTENT, BLOB);
            instance.registerFactory(TypeConstants.EXTERNAL_CONTENT,
                    EXTERNAL_BLOB);
        }
        return instance;
    }

    private DefaultPropertyFactory() {
        super(DEFAULT);
    }

    public void unregisterFactory(String schema, String type) {
        if (schema == null) {
            factories.remove(type);
        } else {
            factories.remove(schema + ':' + type);
        }
    }

    public static DocumentPart newDocumentPart(Schema schema) {
        return new DocumentPartImpl(schema);
    }

    public static DocumentPart newDocumentPart(String schemaName) {
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        Schema schema = mgr != null ? mgr.getSchema(schemaName) : null;
        return newDocumentPart(schema);
    }

    public static Property newMapProperty(Property parent, Field field) {
        return new MapProperty(parent, field);
    }

    public static Property newMapProperty(Property parent, Field field,
            int flags) {
        return new MapProperty(parent, field, flags);
    }

    public static Property newListProperty(Property parent, Field field) {
        return new ListProperty(parent, field);
    }

    public static Property newListProperty(Property parent, Field field,
            int flags) {
        return new ListProperty(parent, field, flags);
    }

    public static Property newScalarProperty(Property parent, Field field) {
        return new ScalarProperty(parent, field);
    }

    public static Property newScalarProperty(Property parent, Field field,
            int flags) {
        return new ScalarProperty(parent, field, flags);
    }

    public static Property newArrayProperty(Property parent, Field field,
            int flags) {
        return new ArrayProperty(parent, field, flags);
    }

    public static Property newProperty(Property parent, Field field, int flags) {
        Property property;
        Type type = field.getType();
        if (type.isSimpleType()) {
            property = newScalarProperty(parent, field, flags);
        } else if (type.isComplexType()) {
            property = newMapProperty(parent, field, flags);
        } else if (type.isListType()) {
            ListType ltype = (ListType) type;
            if (ltype.isArray()) {
                property = newArrayProperty(parent, field, flags);
            } else {
                property = newListProperty(parent, field, flags);
            }
        } else {
            throw new IllegalArgumentException(
                    "Given field type is unsupported: "
                            + field.getType().getName());
        }
        return property;
    }

}

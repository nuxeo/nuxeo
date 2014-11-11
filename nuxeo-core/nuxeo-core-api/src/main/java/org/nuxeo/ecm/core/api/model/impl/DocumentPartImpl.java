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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyDiff;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyFactory;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentPartImpl extends ComplexProperty implements DocumentPart {

    private static final long serialVersionUID = -2959928612693829263L;

    protected transient Schema schema;

    protected transient PropertyFactory factory;


    public DocumentPartImpl(Schema schema, PropertyFactory factory) {
        super(null);
        this.schema = schema;
        this.factory = factory == null ? DefaultPropertyFactory.getInstance() : factory;
    }

    public DocumentPartImpl(Schema schema) {
        this(schema, null);
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public String getName() {
        return schema.getName();
    }

    @Override
    public Schema getType() {
        return schema;
    }

    @Override
    public Field getField() {
        throw new UnsupportedOperationException(
                "Document parts are not bound to schema fields");
    }

    @Override
    public Path collectPath(Path path) {
        return path;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) throws PropertyException {
        visitChildren(visitor, arg);
    }

    @Override
    public Property createProperty(Property parent, Field field) {
        return createProperty(parent, field, 0);
    }

    @Override
    public Property createProperty(Property parent, Field field, int flags) {
        return factory.createProperty(parent, field, flags);
    }

    @Override
    public PropertyDiff exportDiff() {
        return null;
    }

    @Override
    public void importDiff(PropertyDiff diff) {
    }

    private void readObject(ObjectInputStream in)
            throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        in.defaultReadObject();
        try {
            deserialize(in);
        } catch (PropertyException e) {
            IOException ee = new IOException(
                    "failed to deserialize document part " + schema);
            ee.initCause(e);
            throw ee;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        try {
            serialize(out);
        } catch (PropertyException e) {
            IOException ee = new IOException(
                    "failed to serialize document part " + schema);
            ee.initCause(e);
            throw ee;
        }
    }

    public void serialize(ObjectOutputStream out) throws PropertyException, IOException {
        // write schema
        out.writeObject(schema.getName());
        // write factory
        if (factory == null || factory == DefaultPropertyFactory.getInstance()) {
            out.writeObject(null);
        } else if (factory != null) {
            out.writeObject(factory);
        }
        // write children
        Collection<Property> props = getNonPhantomChildren();
        int size = props.size();
        out.writeInt(size);
        if (size > 0) {
            for (Property child : props) {
                serializeProperty(child, out);
            }
        }
    }

    private static void serializeProperty(Property prop, ObjectOutputStream out)
            throws PropertyException, IOException {
        AbstractProperty ap = (AbstractProperty) prop;
        out.writeObject(prop.getName());
        out.writeObject(ap.data);
        out.writeInt(ap.flags);
        if (!prop.isContainer()) {
            out.writeObject(prop.getValue());
            return;
        }
        Collection<Property> props = null;
        if (prop.isList()) {
            props = prop.getChildren();
        } else {
            props = ((ComplexProperty) prop).getNonPhantomChildren();
        }
        int size = props.size();
        out.writeInt(size);
        if (size > 0) {
            for (Property child : props) {
                serializeProperty(child, out);
            }
        }
    }

    public void deserialize(ObjectInputStream in)
            throws ClassNotFoundException, IOException, PropertyException {
        // read schema
        String schemaName = (String) in.readObject();
        //schema = TypeService.getSchemaManager().getSchema(schemaName);
        schema = Framework.getLocalService(SchemaManager.class).getSchema(schemaName);
        // read factory
        factory = (PropertyFactory) in.readObject();
        if (factory == null) {
            factory = DefaultPropertyFactory.getInstance();
        }
        // read children
        deserializeChildren(this, in);
    }


    public void deserializeChildren(ListProperty parent, ObjectInputStream in)
            throws ClassNotFoundException, IOException, PropertyException {
        int size = in.readInt();
        if (size < 1) {
            return;
        }

        Field field = parent.getType().getField();
        for (int i=0; i<size; i++) {
            // read name
            in.readObject(); // name is not used for list children
            Object data = in.readObject();
            // read flags
            int flags = in.readInt();
            Property prop = createProperty(parent, field, flags);
            ((AbstractProperty) prop).data = data;
            if (!prop.isContainer()) {
                prop.init((Serializable) in.readObject());
            } else if (prop.isList()) {
                deserializeChildren((ListProperty) prop, in);
            } else {
                deserializeChildren((ComplexProperty) prop, in);
            }
            // add property to parent
            parent.children.add(prop);
        }
    }

    public void deserializeChildren(ComplexProperty parent, ObjectInputStream in)
            throws ClassNotFoundException, IOException, PropertyException {
        // children are transient so we need to create them explicitely
        parent.children = new Hashtable<String, Property>();
        // read serialized children
        int size = in.readInt();
        if (size < 1) {
            return;
        }
        ComplexType type = parent.getType();
        for (int i=0; i<size; i++) {
            // read name
            String name = (String) in.readObject();
            Object data = in.readObject();
            // read flags
            int flags = in.readInt();
            Property prop = createProperty(parent, type.getField(name), flags);
            ((AbstractProperty) prop).data = data;
            if (!prop.isContainer()) {
                prop.init((Serializable) in.readObject());
            } else if (prop.isList()) {
                deserializeChildren((ListProperty) prop, in);
            } else {
                deserializeChildren((ComplexProperty) prop, in);
            }
            // add property to parent
            parent.children.put(prop.getName(), prop);
        }
    }

    public boolean isSameAs(DocumentPart dp) {
        if (dp == null) {
            return false;
        }
        if (dp instanceof ComplexProperty) {
            return getNonPhantomChildren().equals(
                    ((ComplexProperty) dp).getNonPhantomChildren());
        }
        return false;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getName()
                + (isDirty() ? "*" : "") + ", " + children + ')';
    }

}

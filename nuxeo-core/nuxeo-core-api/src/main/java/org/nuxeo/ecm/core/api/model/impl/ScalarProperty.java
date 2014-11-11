/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 *  A scalar property that is linked to a schema field
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScalarProperty extends AbstractProperty {

    private static final long serialVersionUID = 3078523648297014704L;

    /**
     * The corresponding field.
     */
    protected final Field field;

    protected Serializable value;


    public ScalarProperty(Property parent, Field field) {
        super(parent);
        this.field = field;
    }

    public ScalarProperty(Property parent, Field field, int flags) {
        super(parent, flags);
        this.field = field;
    }


    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
        this.value = value;
    }

    @Override
    public String getName() {
        return field.getName().getPrefixedName();
    }

    @Override
    public Type getType() {
        return field.getType();
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        return value;
    }

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    public Collection<Property> getChildren() {
        throw new UnsupportedOperationException("Scalar properties don't have children");
    }

    @Override
    public Property get(int index) {
        throw new UnsupportedOperationException("Scalar properties don't have children");
    }

    @Override
    public Property get(String name) {
        throw new UnsupportedOperationException("Scalar properties don't have children");
    }

    @Override
    public Property addValue(Object value) {
        throw new UnsupportedOperationException(
                "Scalar properties don't have children");
    }

    @Override
    public Property addValue(int index, Object value) {
        throw new UnsupportedOperationException(
                "Scalar properties don't have children");
    }

    @Override
    public Property addEmpty() {
        throw new UnsupportedOperationException(
                "add() operation not supported on map properties");
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException {
        ScalarProperty clone = (ScalarProperty) super.clone();
        return clone;
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) throws PropertyException {
        visitor.visit(this, arg);
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

    @Override
    public Iterator<Property> getDirtyChildren() {
        throw new UnsupportedOperationException(
                "Cannot iterate over children of scalar properties");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getPath().substring(1)
                + (isDirty() ? "*" : "") + "="
                + (value == null ? "[null]" : value) + ')';
    }

}

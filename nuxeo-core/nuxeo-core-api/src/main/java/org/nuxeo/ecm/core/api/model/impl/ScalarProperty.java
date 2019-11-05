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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * A scalar property that is linked to a schema field
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
    public void set(String name, Property property) throws PropertyException {
        throw new UnsupportedOperationException("Scalar properties don't have children");
    }

    @Override
    public Property addValue(Object value) {
        throw new UnsupportedOperationException("Scalar properties don't have children");
    }

    @Override
    public Property addValue(int index, Object value) {
        throw new UnsupportedOperationException("Scalar properties don't have children");
    }

    @Override
    public Property addEmpty() {
        throw new UnsupportedOperationException("add() operation not supported on map properties");
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
        throw new UnsupportedOperationException("Cannot iterate over children of scalar properties");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getXPath() + (isDirty() ? "*" : "") + "=" + String.valueOf(value)
                + ')';
    }

}

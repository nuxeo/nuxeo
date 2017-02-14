/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api.model.impl.deprecation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.impl.AbstractProperty;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * Property used to declare property removed from schema.
 *
 * @since 9.1
 */
public class RemovedProperty extends AbstractProperty {

    private static final long serialVersionUID = 1L;

    // TODO log could be in SchemaManager when loading deprecated properties
    private static final Log log = LogFactory.getLog(RemovedProperty.class);

    private final Field field;

    public RemovedProperty(Property parent, Field field) {
        super(parent);
        this.field = field;
    }

    public RemovedProperty(Property parent, Field field, int flags) {
        super(parent, flags);
        this.field = field;
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
        log.warn("Field " + getSchema().getName() + ':' + getName()
                + " is marked as removed from schemas, don't use it anymore. Do nothing");
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        log.warn("Field " + getSchema().getName() + ':' + getName()
                + " is marked as removed from schemas, don't use it anymore. Return null.");
        return null;
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
    public boolean isContainer() {
        return false;
    }

    @Override
    public Collection<Property> getChildren() {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Property get(String name) throws PropertyNotFoundException {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Property get(int index) throws PropertyNotFoundException {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Property addValue(Object value) throws PropertyException {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Property addValue(int index, Object value) throws PropertyException {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Property addEmpty() throws PropertyException {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) throws PropertyException {
        // Nothing to do
    }

    @Override
    public boolean isSameAs(Property property) throws PropertyException {
        if (property == null) {
            return false;
        }
        RemovedProperty rp = (RemovedProperty) property;
        return getField().equals(rp.getField());
    }

    @Override
    public Iterator<Property> getDirtyChildren() {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getPath().substring(1) + ")";
    }

}

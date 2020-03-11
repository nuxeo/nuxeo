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

import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * Phantom properties are not stored as children objects.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MapProperty extends ComplexProperty {

    private static final long serialVersionUID = 8061007679778603750L;

    /**
     * The corresponding field.
     */
    protected final Field field;

    public MapProperty(Property parent, Field field) {
        super(parent);
        this.field = field;
    }

    public MapProperty(Property parent, Field field, int flags) {
        super(parent, flags);
        this.field = field;
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public String getName() {
        return field.getName().getPrefixedName();
    }

    @Override
    public ComplexType getType() {
        return (ComplexType) field.getType();
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        MapProperty clone = (MapProperty) super.clone();
        return clone;
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) throws PropertyException {
        arg = visitor.visit(this, arg);
        if (arg != null) {
            visitChildren(visitor, arg);
        }
    }

}

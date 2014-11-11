/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
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

    public boolean isContainer() {
        return true;
    }

    public String getName() {
        return field.getName().getPrefixedName();
    }

    @Override
    public ComplexType getType() {
        return (ComplexType) field.getType();
    }

    public Field getField() {
        return field;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        MapProperty clone = (MapProperty) super.clone();
        return clone;
    }

    public void accept(PropertyVisitor visitor, Object arg) throws PropertyException {
        arg = visitor.visit(this, arg);
        if (arg != null) {
            visitChildren(visitor, arg);
        }
    }

}

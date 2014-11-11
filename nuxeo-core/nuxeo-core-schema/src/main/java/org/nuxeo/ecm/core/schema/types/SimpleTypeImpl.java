/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.schema.types;

import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleTypeImpl extends AbstractType implements SimpleType {

    private static final long serialVersionUID = -5027539058614133891L;

    protected Constraint[] constraints;

    private SimpleType primitiveType;


    public SimpleTypeImpl(SimpleType superType, String schema, String name) {
        this (superType == null ? null : superType.getRef(), schema, name);
    }

    public SimpleTypeImpl(TypeRef<? extends SimpleType> superType, String schema, String name) {
        super(superType, schema, name);
        // simple types must have a not null super type
        // for example a primitive type or another simple type
        assert superType != null;
    }

    protected boolean validateConstraints(Object object) {
        if (constraints != null) {
            for (Constraint constraint : constraints) {
                if (!constraint.validate(object)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean validate(Object object) throws TypeException {
        if (object == null && isNotNull()) {
            return false;
        }
        if (object == null) {
            return true;
        }
        if (validateConstraints(object)) {
            return getSuperType().validate(object);
        }
        return false;
    }

    public void setConstraints(Constraint[] constraints) {
        this.constraints = constraints;
    }

    public SimpleType getPrimitiveType() {
        if (primitiveType == null) {
            primitiveType = ((SimpleType) getSuperType()).getPrimitiveType();
        }
        return primitiveType;
    }

    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isSimpleType() {
        return true;
    }

    @Override
    public Object decode(String str) {
        return getPrimitiveType().decode(str);
    }

    @Override
    public String encode(Object value) {
        return getPrimitiveType().encode(value);
    }

    public Object convert(Object value) throws TypeException {
        return getPrimitiveType().convert(value);
    }

    @Override
    public Object newInstance() {
        // XXX AT: not sure that's what is supposed to be done
        return getPrimitiveType().newInstance();
    }

    @Override
    public TypeRef<? extends SimpleType> getRef() {
        return new TypeRef<SimpleType>(schema, name, this);
    }

}

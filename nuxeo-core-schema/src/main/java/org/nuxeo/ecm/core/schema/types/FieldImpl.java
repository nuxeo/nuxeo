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
public class FieldImpl implements Field {

    private static final long serialVersionUID = 1031109949712043501L;

    private QName name;
    private TypeRef<? extends Type> type;
    private TypeRef<? extends Type> declaringType;
    private int flags;

    private int maxOccurs = 1;
    private int minOccurs = 1;

    private String defaultValue;


    public FieldImpl() {
    }

    public FieldImpl(QName name, Type declaringType, Type type) {
        this(name, declaringType.getRef(), type.getRef(), null, 0);
    }

    public FieldImpl(QName name, Type declaringType, Type type, String defaultValue,
            int flags) {
        this(name, declaringType.getRef(), type.getRef(), defaultValue, flags);
    }

    public FieldImpl(QName name, TypeRef<? extends Type> declaringType,
            TypeRef<? extends Type> type) {
        this(name, declaringType, type, null, 0);
    }

    public FieldImpl(QName name, TypeRef<? extends Type> declaringType,
            TypeRef<? extends Type> type, String defaultValue, int flags) {
        this.name = name;
        this.type = type;
        this.declaringType = declaringType;
        this.defaultValue = defaultValue;
        this.flags = flags;
    }


    public ComplexType getDeclaringType() {
        return (ComplexType) declaringType.get();
    }

    public QName getName() {
        return name;
    }

    public Type getType() {
        return type.get();
    }

    public Object getDefaultValue() {
        return type.get().decode(defaultValue);
    }

    public boolean isNillable() {
        return (flags & NILLABLE) != 0;
    }

    public boolean isConstant() {
        return (flags & CONSTANT) != 0;
    }

    public void setDefaultValue(String value) {
        defaultValue = value;
    }

    public void setNillable(boolean isNillable) {
        if (isNillable) {
            flags |= NILLABLE;
        } else {
            flags &= ~NILLABLE;
        }
    }

    public void setConstant(boolean isConstant) {
        if (isConstant) {
            flags |= CONSTANT;
        } else {
            flags &= ~CONSTANT;
        }
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMaxOccurs(int max) {
        maxOccurs = max;
    }

    public void setMinOccurs(int min) {
        minOccurs = min;
    }

    @Override
    public String toString() {
        return name + " [" + type.getName() + ']';
    }

}

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

    private int maxLength = -1;

    public FieldImpl(QName name, Type declaringType, Type type) {
        this(name, declaringType.getRef(), type.getRef(), null, 0);
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


    @Override
    public ComplexType getDeclaringType() {
        return (ComplexType) declaringType.get();
    }

    @Override
    public QName getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type.get();
    }

    @Override
    public Object getDefaultValue() {
        return type.get().decode(defaultValue);
    }

    @Override
    public boolean isNillable() {
        return (flags & NILLABLE) != 0;
    }

    @Override
    public boolean isConstant() {
        return (flags & CONSTANT) != 0;
    }

    @Override
    public void setDefaultValue(String value) {
        defaultValue = value;
    }

    @Override
    public void setNillable(boolean isNillable) {
        if (isNillable) {
            flags |= NILLABLE;
        } else {
            flags &= ~NILLABLE;
        }
    }

    @Override
    public void setConstant(boolean isConstant) {
        if (isConstant) {
            flags |= CONSTANT;
        } else {
            flags &= ~CONSTANT;
        }
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    @Override
    public void setMaxOccurs(int max) {
        maxOccurs = max;
    }

    @Override
    public void setMinOccurs(int min) {
        minOccurs = min;
    }

    @Override
    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public void setMaxLength(int length) {
        maxLength = length;
    }

    @Override
    public String toString() {
        return name + " [" + type.getName() + ']';
    }

}

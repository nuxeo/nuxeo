/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types;

/**
 * The implementation for a field, which is the association of a type, a name,
 * and default values.
 */
public class FieldImpl implements Field {

    private static final long serialVersionUID = 1L;

    private QName name;

    private Type type;

    private Type declaringType;

    private int flags;

    private int maxOccurs = 1;

    private int minOccurs = 1;

    private String defaultValue;

    private int maxLength = -1;

    public FieldImpl(QName name, Type declaringType, Type type,
            String defaultValue, int flags) {
        this.name = name;
        this.type = type;
        this.declaringType = declaringType;
        this.defaultValue = defaultValue;
        this.flags = flags;
    }

    public FieldImpl(QName name, Type declaringType, Type type) {
        this(name, declaringType, type, null, 0);
    }

    @Override
    public Type getDeclaringType() {
        return declaringType;
    }

    @Override
    public QName getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object getDefaultValue() {
        return type.decode(defaultValue);
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

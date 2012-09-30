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
 * Implementation of a simple type that is not primitive (and therefore has
 * constraints).
 */
public class SimpleTypeImpl extends AbstractType implements SimpleType {

    private static final long serialVersionUID = 1L;

    protected Constraint[] constraints;

    private SimpleType primitiveType;

    public SimpleTypeImpl(SimpleType superType, String schema, String name) {
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

    public Constraint[] getConstraints() {
        return constraints;
    }

    @Override
    public SimpleType getPrimitiveType() {
        if (primitiveType == null) {
            primitiveType = ((SimpleType) getSuperType()).getPrimitiveType();
        }
        return primitiveType;
    }

    @Override
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

    @Override
    public Object convert(Object value) throws TypeException {
        return getPrimitiveType().convert(value);
    }

    @Override
    public Object newInstance() {
        // XXX AT: not sure that's what is supposed to be done
        return getPrimitiveType().newInstance();
    }

}

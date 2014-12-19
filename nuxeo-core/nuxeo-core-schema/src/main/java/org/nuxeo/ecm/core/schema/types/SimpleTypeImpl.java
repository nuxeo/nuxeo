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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema.types;

import java.util.HashSet;

import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

/**
 * Implementation of a simple type that is not primitive (and therefore has constraints).
 */
public class SimpleTypeImpl extends AbstractType implements SimpleType {

    private static final long serialVersionUID = 1L;

    private ObjectResolver resolver;

    private PrimitiveType primitiveType;

    public SimpleTypeImpl(SimpleType superType, String schema, String name) {
        super(superType, schema, name);
        // simple types must have a not null super type
        // for example a primitive type or another simple type
        assert superType != null;
        constraints = new HashSet<Constraint>();
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

    @Override
    public PrimitiveType getPrimitiveType() {
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

    public void setResolver(ObjectResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public ObjectResolver getObjectResolver() {
        return resolver;
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

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.schema.TypeProvider;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract (common) implementation for a Type.
 */
public abstract class AbstractType implements Type {

    private static final long serialVersionUID = 1L;

    public static final Type[] EMPTY_SUPERTYPES = new Type[0];

    protected final String name;

    protected final String schema;

    protected final Type superType;

    protected Set<Constraint> constraints;

    protected AbstractType(Type superType, String schema, String name) {
        this.name = name;
        this.schema = schema;
        this.superType = superType;
        constraints = new HashSet<Constraint>();
    }

    @Override
    public Type getSuperType() {
        return superType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSchemaName() {
        return schema;
    }

    @Override
    public Schema getSchema() {
        return Framework.getLocalService(TypeProvider.class).getSchema(schema);
    }

    @Override
    public boolean isSuperTypeOf(Type type) {
        Type t = type;
        do {
            if (this == t) {
                return true;
            }
            t = t.getSuperType();
        } while (t != null);
        return false;
    }

    public boolean isAny() {
        return false;
    }

    @Override
    public Type[] getTypeHierarchy() {
        Type type = getSuperType();
        if (type == null) {
            return EMPTY_SUPERTYPES;
        }
        List<Type> types = new ArrayList<Type>();
        while (type != null) {
            types.add(type);
            type = type.getSuperType();
        }
        return types.toArray(new Type[types.size()]);
    }

    @Override
    public boolean isSimpleType() {
        return false;
    }

    @Override
    public boolean isComplexType() {
        return false;
    }

    @Override
    public boolean isListType() {
        return false;
    }

    @Override
    public boolean isAnyType() {
        return false;
    }

    @Override
    public boolean isCompositeType() {
        return false;
    }

    @Override
    public boolean validate(Object object) throws TypeException {
        return true;
    }

    @Override
    public Object decode(String string) {
        return null;
    }

    @Override
    public String encode(Object object) {
        return null;
    }

    @Override
    public Object newInstance() {
        return null;
    }

    @Override
    public Set<Constraint> getConstraints() {
        Set<Constraint> constraints = new HashSet<Constraint>();
        if (getSuperType() instanceof SimpleType) {
            SimpleType superType = (SimpleType) getSuperType();
            constraints.addAll(superType.getConstraints());
        }
        constraints.addAll(this.constraints);
        return Collections.unmodifiableSet(constraints);
    }

    public void addConstraints(Collection<Constraint> constraints) {
        this.constraints.addAll(constraints);
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
    public ObjectResolver getObjectResolver() {
        return null;
    }

}

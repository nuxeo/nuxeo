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

import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

/**
 * Primitive type (basic types like long, string, boolean, etc.).
 */
public abstract class PrimitiveType extends AbstractType implements SimpleType {

    private static final long serialVersionUID = -2698475002119528248L;

    protected PrimitiveType(String name) {
        super(null, SchemaNames.BUILTIN, name);
    }

    @Override
    public abstract boolean validate(Object object);

    @Override
    public Type getSuperType() {
        return null;
    }

    @Override
    public Type[] getTypeHierarchy() {
        return EMPTY_SUPERTYPES;
    }

    // FIXME: IType doesn't have an isPrimitive method.
    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isSimpleType() {
        return true;
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return this;
    }

    /**
     * @return true if this primitive types supports this constraints, false otherwise.
     * @since 7.1
     */
    public abstract boolean support(Class<? extends Constraint> constraint);

}

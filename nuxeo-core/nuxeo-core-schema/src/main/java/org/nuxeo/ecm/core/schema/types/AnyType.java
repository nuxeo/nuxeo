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

import org.nuxeo.ecm.core.schema.SchemaNames;

/**
 * Type representing any type (for lists).
 */
public final class AnyType extends AbstractType {

    private static final long serialVersionUID = 1L;

    public static final String ID = "any";

    public static final AnyType INSTANCE = new AnyType();

    private AnyType() {
        super(null, SchemaNames.BUILTIN, ID);
    }

    @Override
    public Type getSuperType() {
        return null;
    }

    @Override
    public Type[] getTypeHierarchy() {
        return EMPTY_SUPERTYPES;
    }

    @Override
    public boolean isAnyType() {
        return true;
    }

    @Override
    public boolean validate(Object object) {
        return true;
    }

    @Override
    public Object convert(Object object) {
        return object;
    }

    protected Object readResolve() {
        return INSTANCE;
    }

}

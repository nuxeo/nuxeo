/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        throw new UnsupportedOperationException("Unimplemented, use DocumentValidationService");
    }

    @Override
    public Object convert(Object object) {
        return object;
    }

    protected Object readResolve() {
        return INSTANCE;
    }

}

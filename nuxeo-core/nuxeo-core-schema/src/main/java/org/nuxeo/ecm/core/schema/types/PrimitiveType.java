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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema.types;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.TypeConstraint;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

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
    public ObjectResolver getObjectResolver() {
        return null;
    }

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

    @Override
    public Set<Constraint> getConstraints() {
        Set<Constraint> constraints = new HashSet<>();
        constraints.add(new TypeConstraint(this));
        return constraints;
    }

}

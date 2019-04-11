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
        constraints = new HashSet<>();
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

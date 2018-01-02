/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.schema.types.primitives;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;

/**
 * The Boolean type.
 */
public final class BooleanType extends PrimitiveType {

    private static final long serialVersionUID = 1L;

    public static final String ID = "boolean";

    public static final BooleanType INSTANCE = new BooleanType();

    private BooleanType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        return object instanceof Boolean;
    }

    @Override
    public Object convert(Object value) {
        if (value instanceof Boolean) {
            return value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0 ? Boolean.TRUE : Boolean.FALSE;
        } else {
            return Boolean.valueOf((String) value);
        }
    }

    @Override
    public Object decode(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        return Boolean.valueOf(str);
    }

    @Override
    public String encode(Object value) {

        if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0 ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
        } else {
            return value != null ? (String) value : "";
        }
    }

    @Override
    public Object newInstance() {
        return Boolean.FALSE;
    }

    protected Object readResolve() {
        return INSTANCE;
    }

    public List<Class<? extends Constraint>> getRelevantConstraints() {
        List<Class<? extends Constraint>> classes = new ArrayList<>();
        classes.add(NotNullConstraint.class);
        return classes;
    }

    @Override
    public boolean support(Class<? extends Constraint> constraint) {
        if (NotNullConstraint.class.equals(constraint)) {
            return true;
        }
        return false;
    }

}

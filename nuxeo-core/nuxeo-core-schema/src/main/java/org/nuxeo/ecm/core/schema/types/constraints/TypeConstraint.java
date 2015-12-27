/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import java.io.Serializable;
import java.util.HashMap;

import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * This constraint ensures some object's is supported by some {@link Type}.
 *
 * @since 7.1
 */
public class TypeConstraint extends AbstractConstraint {

    private static final long serialVersionUID = 1L;

    protected final PrimitiveType type;

    public TypeConstraint(PrimitiveType type) {
        this.type = type;
    }

    @Override
    public boolean validate(Object object) {
        if (object == null) {
            return true;
        }
        return type.validate(object);
    }

    /**
     * <p>
     * Here, value is : <br>
     * name = {@value #NAME} <br>
     * parameters =
     * <ul>
     * <li>{@value #PNAME_TYPE} : org.nuxeo.ecm.core.schema.types.primitives.IntegerType</li>
     * </ul>
     * </p>
     */
    @Override
    public Description getDescription() {
        return new Description(type.getName(), new HashMap<String, Serializable>());
    }

    /**
     * @return The type used by this constraint to validate.
     * @since 7.1
     */
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypeConstraint other = (TypeConstraint) obj;
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

}

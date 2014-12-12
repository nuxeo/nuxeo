/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

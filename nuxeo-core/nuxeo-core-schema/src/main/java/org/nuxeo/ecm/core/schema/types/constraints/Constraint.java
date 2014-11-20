/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - Bogdan Stefanescu <bs@nuxeo.com> - Constraint API skeleton.
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * A constraint object defines a constraint on a custom type. Method {@link #getDescription()} allows anyone to
 * dynamically redefine this constraint in another language (for example in javascript to make client-side validation).
 *
 * @since 7.1
 */
public interface Constraint extends Serializable {

    /**
     * Validates the given object against this constraint.
     * <p>
     * If some object is null. Constraint should return true while validating unless the constraint deals with nullable
     * state.
     * </p>
     *
     * @param object the object to validate
     * @return true if the object was successfully validated, false otherwise
     */
    boolean validate(Object object);

    /**
     * Provides a description of a constraint. For example, a constraint which control String format could return
     * {@code name=PatternMatchingConstraint
     * | parameters= "pattern":"[0-9]+"}
     *
     * @return The constraint description.
     * @since 7.1
     */
    Description getDescription();

    /**
     * Represent the description of a constraint.
     * <p>
     * In the map, key are parameter names, value are parameter values.
     * </p>
     *
     * @since 7.1
     */
    class Description {

        private String name;

        private Map<String, Serializable> parameters;

        public Description(String name, Map<String, Serializable> parameters) {
            super();
            this.name = name;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public Map<String, Serializable> getParameters() {
            return Collections.unmodifiableMap(parameters);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
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
            Description other = (Description) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (parameters == null) {
                if (other.parameters != null) {
                    return false;
                }
            } else if (!parameters.equals(other.parameters)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(name);
            builder.append(parameters.toString());
            return builder.toString();
        }
    }

}

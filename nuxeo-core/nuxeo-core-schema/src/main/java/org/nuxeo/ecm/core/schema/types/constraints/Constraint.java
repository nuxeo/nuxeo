/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - Bogdan Stefanescu <bs@nuxeo.com> - Constraint API skeleton.
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import java.io.Serializable;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * A constraint object defines a constraint on a custom type. Method {@link #getDescription()} allows anyone to
 * dynamically redefine this constraint in another language (for example in javascript to make client-side validation).
 *
 * @since 7.1
 */
public interface Constraint extends Serializable {

    String MESSAGES_BUNDLE = "messages";

    Locale MESSAGES_DEFAULT_LANG = Locale.ENGLISH;

    String MESSAGES_KEY = "label.schema.constraint.violation";

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
     * Provides an error message to display when some invalid value does not match existing entity.
     *
     * @param invalidValue The invalid value that don't match any entity.
     * @param locale The language in which the message should be generated.
     * @return A message in the specified language or
     * @since 7.1
     */
    String getErrorMessage(Object invalidValue, Locale locale);

    /**
     * Provides the message key.
     *
     * @return The message key
     * @since 11.1
     */
    String getMessageKey();

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

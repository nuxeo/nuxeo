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

import java.io.Serializable;
import java.util.Set;

import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

/**
 * A field is a member of a complex type.
 * <p>
 * It is defined by a name and a type.
 */
public interface Field extends Serializable {

    int NILLABLE = 1;

    int CONSTANT = 2;

    int DEPRECATED = 3;

    /**
     * Gets the field name.
     *
     * @return the field name
     */
    QName getName();

    /**
     * Gets the field type.
     *
     * @return the field type
     */
    Type getType();

    /**
     * Gets the complex type or list type that declared this field.
     * <p>
     * The declaring type may differ from the complex type owning this field.
     * <p>
     * For example, in the case of a derived complex type, the field is owned by both the derived type and the base
     * type, but it's declared only by the base type.
     *
     * @return the complex that declared this field
     */
    Type getDeclaringType();

    /**
     * Gets this field default value or null if none.
     *
     * @return the default value if any was specified, null otherwise
     */
    Object getDefaultValue();

    /**
     * Checks whether this field is nillable (can have null values).
     *
     * @return true if the field can have null values
     */
    boolean isNillable();

    /**
     * Checks whether this field is constant (is read only).
     *
     * @return true if the field is constant false otherwise
     */
    boolean isConstant();

    /**
     * Checks whether this field is deprecated.
     *
     * @return true if the field is deprecated false otherwise
     * @since 9.1
     */
    boolean isDeprecated();

    /**
     * Sets the default value of this field.
     *
     * @param value the value to set
     */
    void setDefaultValue(String value);

    /**
     * Sets the nillable flag.
     *
     * @param isNillable
     */
    void setNillable(boolean isNillable);

    /**
     * Sets the constant flag.
     *
     * @param isConstant
     */
    void setConstant(boolean isConstant);

    /**
     * Sets the deprecated flag.
     *
     *  @since 9.1
     */
    void setDeprecated(boolean isDeprecated);

    /**
     * Gets the maximum number this field may occurs in the owner type.
     * <p>
     * By default this is 1. -1 is returned if not a maximum limit is imposed.
     *
     * @return the max occurrences
     */
    int getMaxOccurs();

    /**
     * Gets the minimum number this field may occurs in the owner type.
     * <p>
     * By default this is 1.
     *
     * @return the min occurrences
     */
    int getMinOccurs();

    /**
     * Sets max number of occurrences for this field.
     *
     * @param max max number of occurrences
     */
    void setMaxOccurs(int max);

    /**
     * Sets min number of occurrences for this field.
     *
     * @param min min number of occurrences
     */
    void setMinOccurs(int min);

    /**
     * Gets the maximum length for this field.
     * <p>
     * Value -1 means no constraint.
     *
     * @return the length
     */
    int getMaxLength();

    /**
     * Sets the maximum length for this field.
     *
     * @param length the length, or -1 for no constraint
     */
    void setMaxLength(int length);

    /**
     * @return The constraints applied to this field.
     * @since 7.1
     */
    Set<Constraint> getConstraints();

    /**
     * Used when field is marked as deprecated.
     *
     * @return the fallback xpath to get/set property.
     * @since 9.1
     */
    String getFallbackXpath();

    /**
     * Used when field is marked as deprecated.
     *
     * @since 9.1
     */
    void setFallbackXpath(String fallbackXpath);

}

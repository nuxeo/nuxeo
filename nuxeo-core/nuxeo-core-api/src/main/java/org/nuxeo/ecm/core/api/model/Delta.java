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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model;

/**
 * Value holding a base value and a delta.
 * <p>
 * This is used when the actual intent of the value is to be an incremental update to an existing value.
 *
 * @since 6.0
 */
public abstract class Delta extends Number {

    private static final long serialVersionUID = 1L;

    /**
     * Gets the full value (base + delta) as an object.
     *
     * @return the full value
     */
    public abstract Number getFullValue();

    /**
     * Gets the delta value as an object.
     *
     * @return the delta value
     */
    public abstract Number getDeltaValue();

    /**
     * Gets the base value.
     *
     * @return the base value
     * @since 8.3
     */
    public abstract Number getBase();

    /**
     * Adds this delta to another delta.
     *
     * @param other the other delta
     * @return the added delta
     */
    public abstract Delta add(Delta other);

    /**
     * Adds this delta to a number.
     *
     * @param other the number
     * @return the resulting number
     */
    public abstract Number add(Number other);

    // make these two abstract to force implementation

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

}

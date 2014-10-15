/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model;

/**
 * Value holding a base value and a delta.
 * <p>
 * This is used when the actual intent of the value is to be an incremental
 * update to an existing value.
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
     * Adds two deltas.
     *
     * @param other the other delta
     * @return the added delta
     */
    public abstract Delta add(Delta other);

    // make these two abstract to force implementation

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

}

/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@code Property} gives access to a scalar or array value stored in an
 * underlying table. This base class contains common code.
 * <p>
 * When stored, the values are normalized to their standard type.
 *
 * @author Florent Guillaume
 */
public abstract class BaseProperty {

    /** The property name. */
    protected final String name;

    /** The property type. */
    public final PropertyType type;

    /** Is this property readonly (for system properties). */
    private final boolean readonly;

    /**
     * Creates a Property.
     */
    public BaseProperty(String name, PropertyType type, boolean readonly) {
        this.name = name;
        this.type = type;
        this.readonly = readonly;
    }

    // ----- basics -----

    public String getName() {
        return name;
    }

    // ----- modification -----

    public void refresh(boolean keepChanges) throws StorageException {
        throw new UnsupportedOperationException();
    }

    public void remove() throws StorageException {
        throw new UnsupportedOperationException();
    }

    public void save() throws StorageException {
        throw new UnsupportedOperationException();
    }

    protected void checkWritable() throws StorageException {
        if (readonly) {
            throw new StorageException("Cannot write property: " + name);
        }
    }

}

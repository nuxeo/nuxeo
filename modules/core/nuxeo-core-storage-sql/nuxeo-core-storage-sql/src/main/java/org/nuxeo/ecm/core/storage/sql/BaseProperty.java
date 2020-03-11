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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;

/**
 * A {@code Property} gives access to a scalar or array value stored in an underlying table. This base class contains
 * common code.
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

    public void refresh(boolean keepChanges) {
        throw new UnsupportedOperationException();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void save() {
        throw new UnsupportedOperationException();
    }

    protected void checkWritable() {
        if (readonly) {
            throw new ReadOnlyPropertyException(name);
        }
    }

}

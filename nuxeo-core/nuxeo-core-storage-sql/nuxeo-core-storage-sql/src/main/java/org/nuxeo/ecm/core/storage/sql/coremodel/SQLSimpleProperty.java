/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.SimpleProperty;

/**
 * A {@link SQLSimpleProperty} gives access to a wrapped SQL-level
 * {@link SimpleProperty}.
 *
 * @author Florent Guillaume
 */
public class SQLSimpleProperty extends SQLBaseProperty {

    public static final String DC_ISSUED = "dc:issued";

    protected static final Set<String> VERSION_WRITABLE_PROPS = new HashSet<String>(
            Arrays.asList( //
                    Model.MISC_DIRTY_PROP, //
                    Model.MISC_LIFECYCLE_STATE_PROP, //
                    DC_ISSUED //
            ));

    private final SimpleProperty property;

    /**
     * Creates a {@link SQLSimpleProperty} to wrap a {@link SimpleProperty}.
     */
    public SQLSimpleProperty(SimpleProperty property, Type type,
            boolean readonly) {
        super(type, readonly);
        this.property = property;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public String getName() {
        return property.getName();
    }

    public Serializable getValue() throws DocumentException {
        try {
            return property.getValue();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    public void setValue(Object value) throws DocumentException {
        if (!VERSION_WRITABLE_PROPS.contains(getName())) {
            checkWritable();
        }
        if (value != null && !(value instanceof Serializable)) {
            throw new DocumentException("Value is not Serializable: " + value);
        }
        try {
            property.setValue((Serializable) value);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

}

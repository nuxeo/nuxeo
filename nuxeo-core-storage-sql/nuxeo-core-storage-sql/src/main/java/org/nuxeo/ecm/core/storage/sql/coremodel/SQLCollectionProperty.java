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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.CollectionProperty;

/**
 * A {@link SQLCollectionProperty} gives access to a wrapped SQL-level
 * {@link CollectionProperty}.
 *
 * @author Florent Guillaume
 */
public class SQLCollectionProperty extends SQLBaseProperty {

    private final CollectionProperty property;

    private final boolean isArray;

    /**
     * Creates a {@link SQLCollectionProperty} to wrap a
     * {@link CollectionProperty}.
     */
    public SQLCollectionProperty(CollectionProperty property, ListType type,
            boolean readonly) {
        super(type, readonly);
        this.property = property;
        this.isArray = type == null || type.isArray();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    public String getName() {
        return property.getName();
    }

    public Object getValue() throws DocumentException {
        try {
            Serializable[] value = property.getValue();
            if (isArray) {
                return value;
            } else {
                return new ArrayList<Serializable>(Arrays.asList(value));
            }
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) throws DocumentException {
        checkWritable();
        if (value != null && !(value instanceof Object[])) {
            if (isArray) {
                throw new DocumentException("Value is not Object[] but " +
                        value.getClass().getName() + ": " + value);
            }
            // accept also any List
            if (!(value instanceof Collection)) {
                throw new DocumentException(
                        "Value is not Object[] or Collection but " +
                                value.getClass().getName() + ": " + value);
            }
            value = property.type.getArrayBaseType().collectionToArray(
                    (Collection<Serializable>) value);
        }
        try {
            property.setValue((Object[]) value);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

}

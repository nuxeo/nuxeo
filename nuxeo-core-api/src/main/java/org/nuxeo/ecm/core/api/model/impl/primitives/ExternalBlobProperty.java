/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.osm.ObjectAdapter;
import org.nuxeo.ecm.core.api.model.impl.osm.ObjectMappingError;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * Property handling an external blob: create/edit is done from a map, and the
 * value returned is a blob.
 * <p>
 * Create/edit from a blob is not handled, and the blob uri cannot be retrieved
 * from the blob (no api for now).
 *
 * @author Anahide Tchertchian
 */
public class ExternalBlobProperty extends BlobProperty {

    private static final long serialVersionUID = 1L;

    private static ExternalBlobObjectAdapter adapter;

    public static ObjectAdapter getObjectMetaData() {
        if (adapter == null) {
            try {
                adapter = new ExternalBlobObjectAdapter();
            } catch (Exception e) {
                throw new ObjectMappingError("Failed to map blob property", e);
            }
        }
        return adapter;
    }

    public ExternalBlobProperty(Property parent, Field field, int flags) {
        super(getObjectMetaData(), parent, field, flags);
    }

    public ExternalBlobProperty(ObjectAdapter adapter, Property parent,
            Field field, int flags) {
        super(adapter, parent, field, flags);
    }

    public ExternalBlobProperty(ObjectAdapter adapter, Property parent,
            Field field) {
        super(adapter, parent, field);
    }

    @Override
    public void setValue(Object value) throws PropertyException {
        super.setValue(value);
        // XXX: for some reason, ComplexMemberProperty does not call this method
        // => override it here.
        if (value instanceof Serializable) {
            super.internalSetValue((Serializable) value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Serializable internalGetValue() throws PropertyException {
        // XXX for some reason, the adapter is not used here => override it
        // here...
        Serializable value = super.internalGetValue();
        if (value instanceof Map || value == null) {
            ObjectAdapter adapter = getAdapter();
            return (Serializable) adapter.create((Map) value);
        }
        return null;
    }
}

/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.impl.osm.DynamicObjectAdapter;
import org.nuxeo.ecm.core.api.model.impl.osm.ObjectAdapter;
import org.nuxeo.ecm.core.api.model.impl.osm.ObjectAdapterManager;
import org.nuxeo.ecm.core.api.model.impl.osm.ObjectMappingError;
import org.nuxeo.ecm.core.api.model.impl.osm.ObjectProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BlobProperty extends ObjectProperty {

    private static final long serialVersionUID = 7188847659855943537L;

    private static DynamicObjectAdapter adapter;

    public static ObjectAdapter getObjectMetaData() {
        if (adapter == null) {
            try {
                adapter = new BlobObjectAdapter();
                adapter.addField("length", "length", true);
                adapter.addField("mime-type", "mimeType", true);
                adapter.addField("encoding", "encoding", true);
//                adapter.addField("name", "filename", true);
//                adapter.addField("digest", "digest", true);
                adapter.addField("data", "stream", true);
                ObjectAdapterManager.getInstance().put(Blob.class, adapter);
            } catch (Exception e) {
                throw new ObjectMappingError("Failed to map blob property", e);
            }
        }
        return adapter;
    }

    public BlobProperty(Property parent, Field field, int flags) {
        super (getObjectMetaData(), parent, field, flags);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || ((value instanceof Blob) && (value instanceof Serializable));
    }

    @Override
    public Serializable normalize(Object value)
            throws PropertyConversionException {
        if (isNormalized(value)) {
            //TODO specific blob support?
            return (Serializable)value;
        }
        throw new PropertyConversionException(value.getClass(), Blob.class);
        //TODO byte array is not serializable
//        if (value.getClass() == String.class) {
//            return new ByteArrayInputStream(((String)value).getBytes());
//        }
//        if (value.getClass() == byte[].class) {
//            return new ByteArrayInputStream((byte[])value.);
//        }
    }

    @Override
    public <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException {
        if (value == null) {
            return null;
        }
        if (Blob.class.isAssignableFrom(toType)) {
            return (T) value;
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() throws InstantiationException,
            IllegalAccessException {
        return new ByteArrayInputStream("".getBytes()); // TODO not serializable
    }

}

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
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.impl.primitives.ExternalBlobProperty;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLExternalContentProperty} gives access to a blob, which consists
 * of a {@link Node} of a specialized type: {@code externalcontent}. One of the
 * columns of the row stores the uri that will be used to resolve the actual
 * binary.
 *
 * @author Florent Guillaume
 * @author Anahide Tchertchian
 */
public class SQLExternalContentProperty extends SQLComplexProperty {

    public static final String URI = "uri";

    public SQLExternalContentProperty(Node node, ComplexType type,
            SQLSession session, boolean readonly) {
        super(node, type, session, readonly);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getValue() throws DocumentException {
        Map<String, Object> mapValue = getMapValue();
        if (mapValue != null) {
            return ExternalBlobProperty.getBlobFromMap(mapValue);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMapValue() throws DocumentException {
        Object mapValue = super.getValue();
        if (mapValue instanceof Map) {
            return (Map<String, Object>) mapValue;
        } else if (mapValue != null) {
            throw new DocumentException(
                    "Invalid value for external blob (map needed): " + mapValue);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Object value) throws DocumentException {
        checkWritable();
        if (value == null) {
            // XXX should delete the node?
            for (Property property : getProperties()) {
                property.setValue(null);
            }
        } else {
            if (value instanceof Blob) {
                Property property = getProperty(URI);
                Object uri = property.getValue();
                if (uri == null) {
                    throw new DocumentException(
                            "Cannot set blob properties without "
                                    + "an existing uri set");
                }
                // only update additional properties
                Map<String, Serializable> map = ExternalBlobProperty.getMapFromBlob((Blob) value);
                for (Entry<String, Serializable> entry : map.entrySet()) {
                    String entryKey = entry.getKey();
                    if (entryKey != URI) {
                        property = getProperty(entryKey);
                        property.setValue(entry.getValue());
                    }
                }
            } else if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                for (Entry<String, Object> entry : map.entrySet()) {
                    Property property = getProperty(entry.getKey());
                    property.setValue(entry.getValue());
                }
            } else {
                throw new DocumentException(
                        "Invalid value for external blob (blob or map needed): "
                                + value);
            }
        }
    }

}

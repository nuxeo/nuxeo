/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.CollectionProperty;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.SimpleProperty;

/**
 * @author Florent Guillaume
 */
public class SQLPropertyHelper {

    private SQLPropertyHelper() {
    }

    public static Property getProperty(Node node, Field field)
            throws DocumentException {
        String name = field.getName().getPrefixedName();
        Type type = field.getType();
        if (type.isSimpleType()) {
            SimpleProperty property;
            try {
                property = node.getSimpleProperty(name);
            } catch (StorageException e) {
                throw new DocumentException(e);
            }
            return new SQLSimpleProperty(property, field);
        } else if (type.isListType()) {
            ListType listType = (ListType) type;
            if (listType.getFieldType().isSimpleType()) {
                CollectionProperty property;
                try {
                    property = node.getCollectionProperty(name);
                } catch (StorageException e) {
                    throw new DocumentException(e);
                }
                return new SQLCollectionProperty(property, field);
            } else {
                throw new UnsupportedOperationException("list");
            }
        } else {
            // pass session
            throw new UnsupportedOperationException("complex");
        }
    }

}

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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.search;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * This class is used to convert the field names between the fullname form
 * (schema:field) and prefixed name.
 *
 * @author <a href='mailto:glefter@nuxeo.com'>George Lefter</a>
 */
public final class FieldHelper {

    private FieldHelper() {
    }

    public static String getPrefixedName(String fullName) {
        if (fullName == null) {
            return null;
        }

        if (fullName.startsWith("ecm:")) {
            return fullName;
        }

        int colonIndex = fullName.indexOf(':');
        String schemaName = fullName.substring(0, colonIndex);
        String fieldName = fullName.substring(colonIndex + 1);

        TypeService typeService = (TypeService) Framework.getRuntime().getComponent(
                TypeService.NAME);
        SchemaManager typeManager = typeService.getTypeManager();
        Schema schema = typeManager.getSchema(schemaName);
        if (schema == null) {
            return null;
        }

        Field field = schema.getField(fieldName);

        if (field == null) {
            return null;
        }
        return field.getName().getPrefixedName();
    }

    public static String getFullName(String prefixedName) {
        if (prefixedName == null) {
            return null;
        }
        if (prefixedName.startsWith("ecm:")) {
            return prefixedName;
        }

        TypeService typeService = (TypeService) Framework.getRuntime().getComponent(
                TypeService.NAME);
        SchemaManager typeManager = typeService.getTypeManager();
        Field field = typeManager.getField(prefixedName);
        String schema = field.getDeclaringType().getName();
        String name = field.getName().getLocalName();
        return schema + ':' + name;
    }

}

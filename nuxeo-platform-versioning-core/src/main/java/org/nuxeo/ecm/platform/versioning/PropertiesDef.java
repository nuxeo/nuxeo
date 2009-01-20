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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.versioning;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Defines default values for versions properties.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 * @deprecated use {@link org.nuxeo.ecm.platform.versioning.api.PropertiesDef}
 *             instead
 */
@Deprecated
public final class PropertiesDef {

    public static final String DOC_PROP_MAJOR_VERSION = "uid:major_version";

    public static final String DOC_PROP_MINOR_VERSION = "uid:minor_version";

    private PropertiesDef() {
    }

    /**
     * @deprecated use {@link DocumentModelUtils#getSchemaName(String) instead}
     */
    @Deprecated
    public static String getSchemaName(String propertyName) {
        String[] s = propertyName.split(":");
        String prefix = s[0];
        Schema schema = null;
        try {
            SchemaManager tm = Framework.getService(SchemaManager.class);
            schema = tm.getSchemaFromPrefix(prefix);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (schema == null) {
            // fall back on prefix as it may be the schema name
            return prefix;
        } else {
            return schema.getName();
        }
    }

    /**
     * @deprecated use {@link DocumentModelUtils#getFieldName(String) instead}
     */
    @Deprecated
    public static String getFieldName(String propertyName) {
        String[] s = propertyName.split(":");
        return s[1];
    }

}

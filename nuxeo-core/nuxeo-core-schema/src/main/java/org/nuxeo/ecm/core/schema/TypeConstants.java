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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class TypeConstants {

    public static final String DOCUMENT = "Document";

    public static final String CONTENT_SCHEMA = "file";
    public static final String CONTENT = "content";
    public static final String LIST = "list";

    public static final String ACP = "ecm:acp";
    public static final String ACP_TYPE = "ecmft:acp";

    public static final String SECURITY_SCHEMA_URI = "http://www.nuxeo.org/ecm/schemas/security";
    public static final String SECURITY_SCHEMA_PREFIX = "sec";
    public static final String SECURITY_SCHEMA_NAME = "security";

    private static final Log log = LogFactory.getLog(TypeConstants.class);

    // Constant utility class
    private TypeConstants() {
    }

    public static Type getContentType() {
        try {
            return Framework.getLocalService(SchemaManager.class).getType(
                    CONTENT_SCHEMA,  CONTENT);
        } catch (Throwable e) {
            log.error(e);
        }
        return null;
    }

}

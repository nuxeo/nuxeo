/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class TypeConstants {

    public static final String DOCUMENT = "Document";

    /**
     * @deprecated this is too specific, content can be placed in other schemas.
     */
    @Deprecated
    public static final String CONTENT_SCHEMA = "file";

    public static final String CONTENT = "content";

    public static final String EXTERNAL_CONTENT = "externalcontent";

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

    /**
     * @deprecated this is too specific, content can be placed in other schemas
     *             than the 'file' schema.
     */
    @Deprecated
    public static Type getContentType() {
        try {
            return Framework.getLocalService(SchemaManager.class).getType(
                    CONTENT_SCHEMA, CONTENT);
        } catch (Throwable e) {
            log.error(e);
        }
        return null;
    }

    /**
     * Returns true if given type is named "content", as it's a reserved type
     * name for blobs.
     */
    public static boolean isContentType(Type type) {
        if (type != null && type.getName().equals(TypeConstants.CONTENT)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if given type is named "externalcontent", as it's a reserved
     * type name for external blobs.
     */
    public static boolean isExternalContentType(Type type) {
        if (type != null && type.getName().equals(TypeConstants.EXTERNAL_CONTENT)) {
            return true;
        }
        return false;
    }

}

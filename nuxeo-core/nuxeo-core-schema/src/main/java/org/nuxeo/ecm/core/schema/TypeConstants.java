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

import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class TypeConstants {

    public static final String DOCUMENT = "Document";

    public static final String CONTENT = "content";

    public static final String EXTERNAL_CONTENT = "externalcontent";

    // Constant utility class
    private TypeConstants() {
    }

    /**
     * Returns true if given type is named "content", as it's a reserved type
     * name for blobs.
     */
    public static boolean isContentType(Type type) {
        if (type != null && type.getName().equals(CONTENT)) {
            return true;
        }
        return false;
    }

}

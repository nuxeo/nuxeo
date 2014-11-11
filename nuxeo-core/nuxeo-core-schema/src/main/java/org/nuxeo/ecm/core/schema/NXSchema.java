/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.runtime.api.Framework;

/**
 * Helper to get the {@link SchemaManager} service.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NXSchema {

    // Utility class.
    private NXSchema() {
    }

    public static SchemaManager getSchemaManager() {
        return Framework.getLocalService(SchemaManager.class);
    }

}

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
 * $Id: TypeManagerServiceDelegate.java 21332 2007-06-25 14:36:00Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.client.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Core type manager delegate.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class TypeManagerServiceDelegate {

    private static final Log log = LogFactory.getLog(TypeManagerServiceDelegate.class);

    // Utility class.
    private TypeManagerServiceDelegate() {
    }

    public static SchemaManager getRemoteTypeManagerService() {
        SchemaManager service = null;
        try {
            service = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            log.error("Cannot find distant type manager service");
            log.error(e.getMessage());
        }
        return service;
    }

}

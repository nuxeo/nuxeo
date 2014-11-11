/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SearchServiceDelegate.java 21332 2007-06-25 14:36:00Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.api.delegate;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit logs service delegate.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class AuditLogsServiceDelegate implements Serializable {

    private static final long serialVersionUID = -8140952341564417509L;

    private static final Log log = LogFactory.getLog(AuditLogsServiceDelegate.class);

    // Utility class.
    private AuditLogsServiceDelegate() {
    }

    /**
     * Returns the search service.
     *
     * <p>
     * Returns null if an exception occurs.
     * </p>
     *
     * @return the search service.
     */
    public static Logs getRemoteAuditLogsService() {
        Logs service = null;
        try {
            service = Framework.getService(Logs.class);
        } catch (Exception e) {
            log.error("Cannot find distant audit logs service... ");
            log.error(e.getMessage());
        }
        return service;
    }

}

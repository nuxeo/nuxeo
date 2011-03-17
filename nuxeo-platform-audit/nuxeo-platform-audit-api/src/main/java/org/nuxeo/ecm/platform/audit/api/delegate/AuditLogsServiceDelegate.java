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
 * $Id: SearchServiceDelegate.java 21332 2007-06-25 14:36:00Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.api.delegate;

import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit logs service delegate.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class AuditLogsServiceDelegate {

    // Utility class.
    private AuditLogsServiceDelegate() {
    }

    /**
     * Returns the audit logs service, or null if an exception occurs.
     *
     * @return the audit logs service.
     */
    public static Logs getRemoteAuditLogsService() {
        Logs service;
        try {
            service = Framework.getService(Logs.class);
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot locate remote logs audit", e);
        }
        return service;
    }

}

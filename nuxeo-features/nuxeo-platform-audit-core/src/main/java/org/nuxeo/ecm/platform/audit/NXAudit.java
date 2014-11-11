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
 * $Id: NXAudit.java 20644 2007-06-17 13:15:55Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit;

import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.runtime.api.Framework;

/**
 * Facade for NXRuntime services provided by NXAudit module.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class NXAudit {

    // Utility class.
    private NXAudit() { }

    /**
     * Returns the NXRuntime NXAudit events service.
     *
     * @return the NXRuntime NXAudit events service
     */
    public static NXAuditEvents getNXAuditEventsService() {
        NXAuditEvents service;
        try {
            service = Framework.getService(NXAuditEvents.class);
        } catch (Exception e) {
           throw new AuditRuntimeException("Cannot get audit service", e);
        }
        if (service == null) {
            throw new AuditRuntimeException("Cannot get audit service");
        }
        return service;
    }

}

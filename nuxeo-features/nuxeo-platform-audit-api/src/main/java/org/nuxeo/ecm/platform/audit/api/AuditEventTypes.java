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
 * $Id: AuditEventTypes.java 19481 2007-05-27 10:50:10Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api;

/**
 * NXAudit event types.
 * <p>
 * For now, used by the content history Seam listener to be notified for logs
 * invalidation using the Seam event service.
 * <p>
 * Might useful for other kind of event based notifications.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class AuditEventTypes {

    public static final String HISTORY_CHANGED = "historyChanged";

    // Constant utility class.
    private AuditEventTypes() {
    }

}

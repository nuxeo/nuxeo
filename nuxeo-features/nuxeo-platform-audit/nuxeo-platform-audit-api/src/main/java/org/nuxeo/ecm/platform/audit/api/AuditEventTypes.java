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

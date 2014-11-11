/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.audit.api.document;

import java.util.Date;

/**
 * Simple object used to store the additional parameters that are used to fetch
 * the history for a DocumentModel
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class AdditionalDocumentAuditParams {

    protected String targetUUID;

    protected Date maxDate;

    protected Long eventId;

    public String getTargetUUID() {
        return targetUUID;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    public Long getEventId() {
        return eventId;
    }
}

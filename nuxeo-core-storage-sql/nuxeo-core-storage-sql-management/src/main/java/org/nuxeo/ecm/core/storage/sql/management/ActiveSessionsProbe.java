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
 */

package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeStatus;

/**
 * Simple probes that returns the number of active session
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class ActiveSessionsProbe implements Probe {

    @Override
    public ProbeStatus run() throws ClientException {
        SQLRepositoryStatus status = new SQLRepositoryStatus();
        return ProbeStatus.newSuccess(status.listActiveSessions());
    }

}

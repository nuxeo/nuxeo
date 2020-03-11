/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.runtime.management.api.Probe;
import org.nuxeo.runtime.management.api.ProbeStatus;

/**
 * Simple probes that returns the number of active session
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class ActiveSessionsProbe implements Probe {

    @Override
    public ProbeStatus run() {
        SQLRepositoryStatus status = new SQLRepositoryStatus();
        return ProbeStatus.newSuccess(status.listActiveSessions());
    }

}

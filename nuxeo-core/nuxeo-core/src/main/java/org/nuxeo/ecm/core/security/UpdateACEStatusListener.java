/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener triggering the work updating ACE status.
 *
 * @since 7.4
 */
public class UpdateACEStatusListener implements EventListener {

    public static final String UPDATE_ACE_STATUS_EVENT = "updateACEStatus";

    public void handleEvent(Event event) {
        if (UPDATE_ACE_STATUS_EVENT.equals(event.getName())) {
            UpdateACEStatusWork work = new UpdateACEStatusWork();
            WorkManager workManager = Framework.getLocalService(WorkManager.class);
            workManager.schedule(work, WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        }
    }
}

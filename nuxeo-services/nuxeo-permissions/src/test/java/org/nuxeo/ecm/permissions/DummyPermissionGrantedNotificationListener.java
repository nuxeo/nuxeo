/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.ACL_NAME_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * @since 8.1
 */
public class DummyPermissionGrantedNotificationListener implements PostCommitFilteringEventListener {

    public static List<ACE> processedACEs = new ArrayList<>();

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            handleEvent(event);
        }
    }

    protected void handleEvent(Event event) {
        EventContext eventCtx = event.getContext();
        if (!(eventCtx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) eventCtx;
        ACE ace = (ACE) docCtx.getProperty(ACE_KEY);
        String aclName = (String) docCtx.getProperty(ACL_NAME_KEY);
        if (ace != null && !ace.isDenied() && aclName != null) {
            processedACEs.add(ace);
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        String eventName = event.getName();
        return PERMISSION_NOTIFICATION_EVENT.equals(eventName);
    }
}

/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.user.preferences.listeners;

import static org.nuxeo.ecm.platform.usermanager.UserManagerImpl.ID_PROPERTY_KEY;
import static org.nuxeo.ecm.platform.usermanager.UserManagerImpl.USERDELETED_EVENT_ID;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;
import org.nuxeo.user.preferences.api.UserPreferencesService;

/**
 * Deletes all user preferences of a deleted user.
 *
 * @since 2025.16
 */
public class UserDeletedListener implements PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle events) {
        if (events.containsEventName(USERDELETED_EVENT_ID)) {
            for (Event event : events) {
                handleEvent(event);
            }
        }
    }

    public void handleEvent(Event event) {
        if (!USERDELETED_EVENT_ID.equals(event.getName())) {
            return;
        }
        var repositoryService = Framework.getService(RepositoryService.class);
        List<String> repositoryNames = repositoryService.getRepositoryNames();
        var ups = Framework.getService(UserPreferencesService.class);
        for (String repositoryName : repositoryNames) {
            try (NuxeoLoginContext ignored = Framework.loginSystem()) {
                ups.deleteAllForUser(CoreInstance.getCoreSession(repositoryName),
                        (String) event.getContext().getProperty(ID_PROPERTY_KEY));
            }
        }
    }
}

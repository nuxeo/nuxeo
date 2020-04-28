/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.coldstorage.events;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.coldstorage.helpers.ColdStorageHelper;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * A synchronous listener that checks if the contents being retrieved form cold storage are available.
 *
 * @apiNote: This listener is designed to be called from a scheduler.
 * @since 11.1
 */
public class CheckColdStorageContentAvailabilityListener implements EventListener {

    private static final Logger log = LogManager.getLogger(CheckColdStorageContentAvailabilityListener.class);

    @Override
    public void handleEvent(final Event event) {
        log.debug("Start checking the available cold storage content");
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        for (String repository : repositoryNames) {
            CoreSession coreSession = CoreInstance.getCoreSession(repository);
            ColdStorageHelper.checkColdStorageContentAvailability(coreSession);
        }
        log.debug("End checking the available cold storage content");
    }
}

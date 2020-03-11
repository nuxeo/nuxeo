/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.rendition.listener;

import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.4
 */
public class StoredRenditionsCleanupListener implements PostCommitFilteringEventListener {

    public static final String STORED_RENDITIONS_CLEANUP_EVENT = "storedRenditionsCleanup";

    @Override
    public void handleEvent(EventBundle eventBundle) {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        RenditionService renditionService = Framework.getService(RenditionService.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            renditionService.deleteStoredRenditions(repositoryName);
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        return STORED_RENDITIONS_CLEANUP_EVENT.equals(event.getName());
    }

}

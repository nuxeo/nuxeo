/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * Runtime listener for handling reload of {@code changeFinder} contributions.
 *
 * @author Antoine Taillefer
 * @see NuxeoDriveManagerImpl
 * @since 7.3
 */
public class ChangeFinderReloadListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (ReloadService.RELOAD_EVENT_ID.equals(event.getId())) {
            NuxeoDriveManagerImpl nuxeoDriveManager = (NuxeoDriveManagerImpl) Framework.getService(
                    NuxeoDriveManager.class);
            nuxeoDriveManager.initChangeFinder();
        }
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

}

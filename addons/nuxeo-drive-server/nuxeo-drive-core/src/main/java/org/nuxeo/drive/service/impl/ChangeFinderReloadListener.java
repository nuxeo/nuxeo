/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
            NuxeoDriveManagerImpl nuxeoDriveManager = (NuxeoDriveManagerImpl) Framework.getService(NuxeoDriveManager.class);
            nuxeoDriveManager.initChangeFinder();
        }
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

}

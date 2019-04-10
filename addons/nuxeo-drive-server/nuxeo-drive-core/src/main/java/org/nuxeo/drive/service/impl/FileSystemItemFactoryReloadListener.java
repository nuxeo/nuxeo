/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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

import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * Runtime listener for handling reload of {@code fileSystemItemFactory} and {@code topLevelFolderItemFactory}
 * contributions.
 * 
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public class FileSystemItemFactoryReloadListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (ReloadService.RELOAD_EVENT_ID.equals(event.getId())) {
            try {
                FileSystemItemAdapterServiceImpl fileSystemItemAdapterService = (FileSystemItemAdapterServiceImpl) Framework.getLocalService(FileSystemItemAdapterService.class);
                fileSystemItemAdapterService.setActiveFactories();
            } catch (Exception e) {
                throw new ClientRuntimeException("Cannot sort fileSystemItemFactory contributions on reload.", e);
            }
        }
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

}

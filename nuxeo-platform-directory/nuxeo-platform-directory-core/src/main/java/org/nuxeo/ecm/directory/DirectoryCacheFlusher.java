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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadEventNames;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * Event listener that flushes the {@link DirectoryService} caches.
 *
 * @since 5.6
 */
public class DirectoryCacheFlusher implements EventListener {

    private static final Log log = LogFactory.getLog(DirectoryCacheFlusher.class);

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    @Override
    public void handleEvent(Event event) {
        if (!Framework.isDevModeSet()) {
            log.info("Do not flush the directory caches: dev mode is not set");
            return;
        }
        if (!ReloadEventNames.FLUSH_EVENT_ID.equals(event.getId())) {
            return;
        }
        try {
            DirectoryService service = Framework.getLocalService(DirectoryService.class);
            List<Directory> directories = service.getDirectories();
            if (directories != null) {
                for (Directory directory : directories) {
                    directory.getCache().invalidateAll();
                }
            }
        } catch (Exception e) {
            log.error("Error while flushing the directory caches", e);
        }
    }

}

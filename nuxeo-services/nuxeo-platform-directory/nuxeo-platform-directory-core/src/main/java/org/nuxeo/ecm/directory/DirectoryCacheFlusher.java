/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
        } catch (DirectoryException e) {
            log.error("Error while flushing the directory caches", e);
        }
    }

}

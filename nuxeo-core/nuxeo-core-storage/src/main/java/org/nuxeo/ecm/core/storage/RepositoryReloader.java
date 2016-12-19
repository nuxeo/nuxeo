/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

public class RepositoryReloader implements EventListener {

    private static Log log = LogFactory.getLog(RepositoryReloader.class);

    @Override
    public void handleEvent(Event event) {
        final String id = event.getId();
        if (ReloadService.RELOAD_REPOSITORIES_ID.equals(id) || ReloadService.FLUSH_EVENT_ID.equals(id)) {
            reloadRepositories();
        }
    }

    protected static void closeRepositories() {
        Framework.getService(RepositoryService.class).shutdown();
    }

    protected static void flushJCAPool() {
        try {
            Class<?> nuxeoContainerClass = Class.forName("org.nuxeo.runtime.jtajca.NuxeoContainer");
            if (nuxeoContainerClass != null) {
                nuxeoContainerClass.getMethod("resetConnectionManager").invoke(null);
            }
        } catch (ClassNotFoundException e) {
            // no container
            log.debug(e, e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reload core repositories.
     */
    protected static void reloadRepositories() {
        RepositoryReloader.closeRepositories();
    }
}

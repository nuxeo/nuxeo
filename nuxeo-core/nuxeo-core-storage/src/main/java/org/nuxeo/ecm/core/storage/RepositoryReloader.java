/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    public boolean aboutToHandleEvent(Event event) {
        return true;
    }

    @Override
    public void handleEvent(Event event) {
        final String id = event.getId();
        if (ReloadService.RELOAD_REPOSITORIES_ID.equals(id)
                || ReloadService.FLUSH_EVENT_ID.equals(id)) {
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
                nuxeoContainerClass.getMethod("resetConnectionManager").invoke(
                        null);
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

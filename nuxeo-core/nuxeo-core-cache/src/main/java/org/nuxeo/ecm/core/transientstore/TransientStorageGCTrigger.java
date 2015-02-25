/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.transientstore;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple  Listener used to GC Work for {@link TransientStore}s
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class TransientStorageGCTrigger implements EventListener {

    public static final String EVENT = "transientStoreGCStart";

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (event.getName().equals(EVENT)) {

            WorkManager wm = Framework.getService(WorkManager.class);
            wm.schedule(new AbstractWork() {

                private static final long serialVersionUID = 1L;

                @Override
                public String getTitle() {
                    return "Transient Store GC";
                }

                @Override
                public void work() {
                    TransientStoreService tss = Framework.getService(TransientStoreService.class);
                    tss.doGC();
                }
            }, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        }
    }
}

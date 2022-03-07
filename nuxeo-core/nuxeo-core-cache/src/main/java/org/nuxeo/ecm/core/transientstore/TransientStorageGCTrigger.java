/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.transientstore;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple Listener used to GC Work for {@link TransientStore}s
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class TransientStorageGCTrigger implements EventListener {

    public static final String EVENT = "transientStoreGCStart";

    @Override
    public void handleEvent(Event event) {
        if (event.getName().equals(EVENT)) {
            WorkManager wm = Framework.getService(WorkManager.class);
            TransientStoreService tss = Framework.getService(TransientStoreService.class);
            tss.listStores().forEach(store -> wm.schedule(new TransiantStorageGCWork(store)));
        }
    }
}

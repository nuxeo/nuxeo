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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.listener;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.threed.service.ThreeDBatchUpdateWork;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_FACET;
import static org.nuxeo.ecm.platform.threed.listener.ThreeDBatchCleanerListener.GENERATE_BATCH_DATA;

/**
 * Listener batch updating transmission formats and renders if the main Blob has changed.
 *
 * @since 8.4
 */
public class ThreeDBatchGenerationListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        Boolean generate = (Boolean) event.getContext().getProperty(GENERATE_BATCH_DATA);
        if (!Boolean.TRUE.equals(generate)) {
            // ignore the event - we are blocked by the caller
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(THREED_FACET) && !doc.isProxy()) {
            ThreeDBatchUpdateWork work = new ThreeDBatchUpdateWork(doc.getRepositoryName(), doc.getId());
            WorkManager manager = Framework.getService(WorkManager.class);

            ThreeDBatchUpdateWork running = (ThreeDBatchUpdateWork) manager.find(work.getId(), Work.State.RUNNING);
            ThreeDBatchUpdateWork scheduled = (ThreeDBatchUpdateWork) manager.find(work.getId(), Work.State.SCHEDULED);
            if (running != null) {
                running.suspended();
                running.setStatus("Suspended");
            } else if (scheduled != null) {
                scheduled.suspended();
                scheduled.setStatus("Suspended");
            }

            manager.schedule(work, WorkManager.Scheduling.ENQUEUE, true);
        }
    }
}

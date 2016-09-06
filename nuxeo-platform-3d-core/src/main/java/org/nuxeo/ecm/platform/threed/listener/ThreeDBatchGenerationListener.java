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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.threed.service.ThreeDBatchUpdateWork;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_FACET;
import static org.nuxeo.ecm.platform.threed.listener.ThreeDBatchCleanerListener.CLEAN_BATCH_DATA;

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

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(THREED_FACET) && !doc.isProxy()) {
            Property origThreeDProperty = doc.getProperty("file:content");
            Blob threedMain = (Blob) origThreeDProperty.getValue();
            if ((origThreeDProperty.isDirty() || doc.getProperty("files:files").isDirty()) && threedMain != null) {
                docCtx.setProperty(CLEAN_BATCH_DATA, true);
                ThreeDBatchUpdateWork work = new ThreeDBatchUpdateWork(doc.getRepositoryName(), doc.getId());
                WorkManager workManager = Framework.getLocalService(WorkManager.class);
                workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
            }
        }
    }
}

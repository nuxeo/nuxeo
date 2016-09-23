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
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_FACET;

/**
 * Listener cleaning batch date when batch update is scheduled.
 *
 * @since 8.4
 */
public class ThreeDBatchCleanerListener implements EventListener {

    public static final String GENERATE_BATCH_DATA = "generateBatchData";

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(THREED_FACET) && !doc.isProxy()) {
            Property origThreeDProperty = doc.getProperty("file:content");
            Blob threedMain = (Blob) origThreeDProperty.getValue();
            if ((origThreeDProperty.isDirty() || doc.getProperty("files:files").isDirty()) && threedMain != null) {
                ThreeDService threeDService = Framework.getService(ThreeDService.class);
                threeDService.cleanBatchData(doc);
                docCtx.setProperty(GENERATE_BATCH_DATA, true);
            }
        }
    }
}

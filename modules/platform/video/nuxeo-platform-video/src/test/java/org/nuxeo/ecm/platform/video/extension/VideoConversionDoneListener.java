/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.video.extension;

import static org.nuxeo.ecm.platform.video.service.VideoConversionWork.VIDEO_CONVERSIONS_DONE_EVENT;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * A test listener that catches {@code videoConversionsDone} event.
 *
 * @since 11.1
 */
public class VideoConversionDoneListener implements EventListener {

    private static final Log log = LogFactory.getLog(VideoConversionDoneListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        String eventName = event.getName();
        if (!VIDEO_CONVERSIONS_DONE_EVENT.equals(eventName)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        incrementEventCount(doc.getId());
    }

    protected void incrementEventCount(String id) {
        KeyValueStore kv = Framework.getService(KeyValueService.class).getKeyValueStore("default");
        long count = kv.addAndGet(id, 1);
        log.info(String.format("Increment conversion done for doc: %s, count: %d", id, count));
    }
}

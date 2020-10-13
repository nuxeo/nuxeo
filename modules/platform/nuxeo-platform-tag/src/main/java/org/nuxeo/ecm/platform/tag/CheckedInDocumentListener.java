/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.tag;

import static org.nuxeo.ecm.platform.tag.TagService.Feature.TAGS_BELONG_TO_DOCUMENT;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that copy tags applied on the live document to a version synchronously after the check-in event.
 *
 * @since 9.3
 */
public class CheckedInDocumentListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            CoreSession session = docCtx.getCoreSession();
            DocumentModel doc = docCtx.getSourceDocument();
            if (doc == null) {
                return;
            }
            String docId = doc.getId();
            TagService tagService = Framework.getService(TagService.class);
            if (doc instanceof DeletedDocumentModel) {
                tagService.removeTags(session, docId);
                return;
            }
            DocumentRef versionRef = (DocumentRef) ctx.getProperty("checkedInVersionRef");
            if (!tagService.hasFeature(TAGS_BELONG_TO_DOCUMENT)) {
                if (versionRef instanceof IdRef) {
                    tagService.copyTags(session, docId, versionRef.toString());
                }
            }
        }
    }
}

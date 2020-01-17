/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.tag;

import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_RESTORED;
import static org.nuxeo.ecm.core.api.trash.TrashService.DOCUMENT_TRASHED;
import static org.nuxeo.ecm.platform.tag.TagService.Feature.TAGS_BELONG_TO_DOCUMENT;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Listener that copy tags applied on the live document to a version or proxy of this document or replace the existing
 * tags on a live document by the ones on the version being restored.
 *
 * @since 5.7.3
 */
public class TaggedVersionListener implements PostCommitFilteringEventListener {

    protected static final String TAGS_REMOVAL_ON_TRASH_PROP = "nuxeo.tag.removal.on.trash.enabled";

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            if (acceptEvent(event)) {
                handleEvent(event);
            }
        }
    }

    protected void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            String name = event.getName();
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            CoreSession session = docCtx.getCoreSession();
            DocumentModel doc = docCtx.getSourceDocument();
            if (doc == null) {
                return;
            }
            String docId = doc.getId();
            TagService tagService = Framework.getService(TagService.class);
            if (doc instanceof DeletedDocumentModel) {
                if (!tagService.hasFeature(TAGS_BELONG_TO_DOCUMENT)) {
                    tagService.removeTags(session, docId);
                    return;
                }
            }
            switch (name) {
            case DOCUMENT_PROXY_PUBLISHED:
                if (doc.isProxy()) {
                    DocumentModel version = session.getSourceDocument(doc.getRef());
                    tagService.copyTags(session, version.getId(), docId);
                }
                break;
            case DOCUMENT_RESTORED:
                String versionUUID = (String) ctx.getProperty(VersioningDocument.RESTORED_VERSION_UUID_KEY);
                tagService.replaceTags(session, versionUUID, docId);
                break;
            case DOCUMENT_REMOVED:
                if (!tagService.hasFeature(TAGS_BELONG_TO_DOCUMENT)) {
                    tagService.removeTags(session, docId);
                }
                break;
            case DOCUMENT_TRASHED:
            case TRANSITION_EVENT:
                if (Framework.getService(ConfigurationService.class).isBooleanTrue(TAGS_REMOVAL_ON_TRASH_PROP)) {
                    tagService.removeTags(session, docId);
                }
                break;
            default:
                break;
            }
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        String name = event.getName();
        return DOCUMENT_PROXY_PUBLISHED.equals(name)
                || DOCUMENT_RESTORED.equals(name)
                || DOCUMENT_REMOVED.equals(name)
                || DOCUMENT_TRASHED.equals(name)
                || (LifeCycleConstants.TRANSITION_EVENT.equals(name) && LifeCycleConstants.DELETED_STATE.equals(event.getContext().getProperty(
                LifeCycleConstants.TRANSTION_EVENT_OPTION_TO)));
    }
}

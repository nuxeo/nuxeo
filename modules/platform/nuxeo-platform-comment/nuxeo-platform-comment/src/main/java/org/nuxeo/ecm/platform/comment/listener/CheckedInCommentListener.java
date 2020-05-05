/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ANCESTOR_IDS_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.impl.AbstractCommentManager.COMMENTS_DIRECTORY;

import java.util.ArrayList;
import java.util.Arrays;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;

/**
 * Listener that updates {@code comment:parentId} and {@code comment:ancestorIds} on version's comments after the
 * check-in event.
 *
 * @since 11.1
 */
public class CheckedInCommentListener implements PostCommitFilteringEventListener {

    @Override
    public boolean acceptEvent(Event event) {
        return DOCUMENT_CHECKEDIN.equals(event.getName());
    }

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
        CoreSession session = ctx.getCoreSession();
        DocumentRef versionRef = (DocumentRef) ctx.getProperty("checkedInVersionRef");
        if (versionRef != null && session.hasChild(versionRef, COMMENTS_DIRECTORY)) {
            DocumentModel comments = session.getChild(versionRef, COMMENTS_DIRECTORY);
            updateCommentProperties(session, comments.getId(), versionRef.reference().toString());
        }
    }

    protected void updateCommentProperties(CoreSession session, String ecmParentId, String commentParentId,
            String... parentCommentAncestorIds) {
        int limit = 100;
        long offset = 0;
        long total = 0;
        do {
            DocumentModelList docModels = session.query(
                    String.format("SELECT * FROM Comment where ecm:parentId='%s'", ecmParentId), null, limit, offset,
                    true);
            for (DocumentModel docModel : docModels) {
                // build current ancestor ids
                var commentAncestorIds = new ArrayList<>(Arrays.asList(parentCommentAncestorIds));
                commentAncestorIds.add(commentParentId);
                docModel.setPropertyValue(COMMENT_ANCESTOR_IDS_PROPERTY, commentAncestorIds);
                docModel.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, commentParentId);
                session.saveDocument(docModel);
                // loop on replies
                updateCommentProperties(session, docModel.getId(), docModel.getId(),
                        commentAncestorIds.toArray(String[]::new));
            }
            offset += limit;
            if (total == 0) {
                total = docModels.totalSize();
            }
            session.save();
        } while (offset < total);
    }
}

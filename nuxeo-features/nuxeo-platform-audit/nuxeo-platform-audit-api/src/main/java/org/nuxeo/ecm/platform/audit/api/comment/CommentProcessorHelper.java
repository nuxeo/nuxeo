/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.audit.api.comment;

import java.util.List;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Helper to manage {@link LogEntry} comment processing (code was moved from the Seam bean)
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class CommentProcessorHelper {

    protected CoreSession documentManager;

    public CommentProcessorHelper(CoreSession documentManager) {
        this.documentManager = documentManager;
    }

    public void processComments(List<LogEntry> logEntries) {
        if (logEntries == null) {
            return;
        }
        for (LogEntry entry : logEntries) {
            String comment = getLogComment(entry);
            LinkedDocument linkedDoc = getLogLinkedDocument(entry);
            entry.setPreprocessedComment(new UIAuditComment(comment, linkedDoc));
        }
    }

    public String getLogComment(LogEntry entry) {
        String oldComment = entry.getComment();
        if (oldComment == null) {
            return null;
        }

        String newComment = oldComment;
        boolean targetDocExists = false;
        String[] split = oldComment.split(":");
        if (split.length >= 2) {
            String strDocRef = split[1];
            DocumentRef docRef = new IdRef(strDocRef);
            targetDocExists = documentManager.exists(docRef);
        }

        if (targetDocExists) {
            String eventId = entry.getEventId();
            // update comment
            if (DocumentEventTypes.DOCUMENT_DUPLICATED.equals(eventId)) {
                newComment = "audit.duplicated_to";
            } else if (DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(eventId)) {
                newComment = "audit.copied_from";
            } else if (DocumentEventTypes.DOCUMENT_MOVED.equals(eventId)) {
                newComment = "audit.moved_from";
            }
        }

        return newComment;
    }

    public LinkedDocument getLogLinkedDocument(LogEntry entry) {
        String oldComment = entry.getComment();
        if (oldComment == null) {
            return null;
        }

        LinkedDocument linkedDoc = null;

        String[] split = oldComment.split(":");
        if (split.length >= 2) {
            String repoName = split[0];
            String strDocRef = split[1];

            // test if strDocRef is a document uuid to continue
            if (IdUtils.isValidUUID(strDocRef)) {
                DocumentRef docRef = new IdRef(strDocRef);
                RepositoryLocation repoLoc = new RepositoryLocation(repoName);

                // create linked doc, broken by default
                linkedDoc = new LinkedDocument();
                linkedDoc.setDocumentRef(docRef);
                linkedDoc.setRepository(repoLoc);

                // try to resolve target document
                // XXX multi-repository management
                try {
                    DocumentModel targetDoc = documentManager.getDocument(docRef);
                    linkedDoc.setDocument(targetDoc);
                    linkedDoc.setBrokenDocument(false);
                } catch (DocumentNotFoundException e) {
                    // not the expected format or broken document
                }
            }
        }

        return linkedDoc;
    }

}

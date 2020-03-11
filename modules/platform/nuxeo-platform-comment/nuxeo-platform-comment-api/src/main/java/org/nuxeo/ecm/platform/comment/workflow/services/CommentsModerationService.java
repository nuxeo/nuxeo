/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.comment.workflow.services;

import java.util.ArrayList;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface CommentsModerationService {

    /**
     * Starts the moderation process on given Comment posted on a documentModel.
     *
     * @param session the coreSession
     * @param document the document were the comment is posted
     * @param commentId the commentId
     */
    void startModeration(CoreSession session, DocumentModel document, String commentId, ArrayList<String> moderators);

    /**
     * Approve the comment with the given commentId.
     *
     * @param session the coreSession
     * @param document the document were the comment is posted
     * @param commentId the commentId
     */
    void approveComent(CoreSession session, DocumentModel document, String commentId);

    /**
     * Reject the comment with the given commentId.
     *
     * @param session the coreSession
     * @param document the document were the comment is posted
     * @param commentId the commentId
     */
    void rejectComment(CoreSession session, DocumentModel document, String commentId);

    /**
     * Publish the given comment.
     *
     * @param session the coreSession
     * @param comment the comment to publish
     */
    void publishComment(CoreSession session, DocumentModel comment);

}

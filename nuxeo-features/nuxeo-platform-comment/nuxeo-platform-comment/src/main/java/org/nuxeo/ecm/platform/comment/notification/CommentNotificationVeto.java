/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.comment.notification;

import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;

/**
 * Veto that prevents sending notifications whose type is {@link #getExcludedEventType} on the document comment model.
 * 
 * @since 11.1
 */
public interface CommentNotificationVeto extends NotificationListenerVeto {

    Set<String> ACCEPTED_COMMENT_TYPE = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(COMMENT_DOC_TYPE, ANNOTATION_DOC_TYPE, "Post")));

    /**
     * @return the event type that we don't want to notify
     */
    String getExcludedEventType();

    @Override
    default boolean accept(Event event) {
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return false;
        }
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        String eventType = getExcludedEventType();
        String docType = docCtx.getSourceDocument().getType();

        return eventType.equals(event.getName()) && (ACCEPTED_COMMENT_TYPE.contains(docType));
    }
}

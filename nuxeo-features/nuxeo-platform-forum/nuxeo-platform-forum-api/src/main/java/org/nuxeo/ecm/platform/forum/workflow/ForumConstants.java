/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bchaffangeon
 *
 * $Id: ForumConstants.java 19759 2007-05-31 11:11:12Z janguenot $
 */

package org.nuxeo.ecm.platform.forum.workflow;

/**
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public final class ForumConstants {

    /**
     * @deprecated use jbpmService variable "participants" instead
     */
    @Deprecated
    public static final String FORUM_MODERATORS_LIST = "moderatorsList";

    public static final String PENDING_STATE = "moderation_pending";

    public static final String PUBLISHED_STATE = "moderation_published";

    public static final String TRANSITION_TO_PUBLISHED_STATE = "moderation_publish";

    public static final String TRANSITION_TO_REJECTED_STATE = "moderation_reject";

    public static final String PROCESS_INSTANCE_NAME = "forum_moderation";

    public static final String COMMENT_ID = "commentId";

    public static final String THREAD_REF = "threadRef";

    public static final String PROCESS_TRANSITION_TO_PUBLISH = "moderation_publish";

    public static final String PROCESS_TRANSITION_TO_REJECTED = "moderation_reject";

    public static final String MODERATION_TASK_NAME = "moderate";

    public static final String FORUM_TASK_TYPE = "forum_moderate";

    // Constant utility class.
    private ForumConstants() {
    }

}

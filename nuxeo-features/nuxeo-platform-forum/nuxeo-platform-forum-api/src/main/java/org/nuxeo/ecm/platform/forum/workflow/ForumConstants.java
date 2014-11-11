/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

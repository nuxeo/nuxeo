/*
 * (C) Copyright 2009-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuno Cunha <ncunha@nuxeo.com>
 */
package org.nuxeo.ecm.platform.comment.workflow.utils;

import org.nuxeo.ecm.platform.comment.api.CommentConstants;

/**
 * Utility class used for registering constants.
 */
public final class CommentsConstants {

    /*
     * Constants used for Comments
     */
    public static final String PERMISSION_COMMENT = "Comment";

    public static final String PERMISSION_MODERATE = "Moderate";

    public static final String PERMISSION_WRITE = "Write";

    public static final String PERMISSION_MANAGE_EVERYTHING = "Everything";

    public static final String TRANSITION_TO_PUBLISHED_STATE = "moderation_publish";

    public static final String PUBLISHED_STATE = "moderation_published";

    public static final String PENDING_STATE = "moderation_pending";

    public static final String REJECT_STATE = "moderation_reject";

    public static final String MODERATION_PROCESS = "comments_moderation";

    public static final String COMMENT_ID = "commentId";

    public static final String COMMENT_PUBLISHED = "commentPublished";

    public static final String COMMENT_LIFECYCLE = "comment_moderation";

    public static final String ACCEPT_CHAIN_NAME = "acceptComment";

    public static final String REJECT_CHAIN_NAME = "rejectComment";

    public static final String MODERATION_DIRECTIVE_NAME = "moderate";

    /**
     * Schemas and fields.
     * 
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_DOC_TYPE} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_DOC_TYPE = CommentConstants.COMMENT_DOC_TYPE;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_SCHEMA} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_SCHEMA = CommentConstants.COMMENT_SCHEMA;

    /**
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_CREATION_DATE_PROPERTY} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_CREATION_DATE = CommentConstants.COMMENT_CREATION_DATE_PROPERTY;

    /**
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_AUTHOR_PROPERTY} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_AUTHOR = CommentConstants.COMMENT_AUTHOR_PROPERTY;

    /**
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_TEXT_PROPERTY} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_TEXT = CommentConstants.COMMENT_TEXT_PROPERTY;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_PARENT_ID_PROPERTY} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_PARENT_ID = CommentConstants.COMMENT_PARENT_ID_PROPERTY;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_MODIFICATION_DATE_PROPERTY} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_MODIFICATION_DATE = CommentConstants.COMMENT_MODIFICATION_DATE_PROPERTY;

    /**
     /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_ANCESTOR_IDS_PROPERTY} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_ANCESTOR_IDS = CommentConstants.COMMENT_ANCESTOR_IDS_PROPERTY;

    // JSON constant part

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_ENTITY_TYPE} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_ENTITY_TYPE = CommentConstants.COMMENT_ENTITY_TYPE;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENTS_ENTITY_TYPE} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENTS_ENTITY_TYPE = CommentConstants.COMMENTS_ENTITY_TYPE;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_ID_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_ID_FIELD = CommentConstants.COMMENT_ID_FIELD;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_PARENT_ID_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_PARENT_ID_FIELD = CommentConstants.COMMENT_PARENT_ID_FIELD;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_ANCESTOR_IDS_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_ANCESTOR_IDS_FIELD = CommentConstants.COMMENT_ANCESTOR_IDS_FIELD;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_AUTHOR_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_AUTHOR_FIELD = CommentConstants.COMMENT_AUTHOR_FIELD;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_TEXT_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_TEXT_FIELD = CommentConstants.COMMENT_TEXT_FIELD;

    /**
     * Creation Date in ISO-8601 representation.
     * 
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_CREATION_DATE_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_CREATION_DATE_FIELD = CommentConstants.COMMENT_CREATION_DATE_FIELD;

    /**
     * Modification Date in ISO-8601 representation.
     * 
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_MODIFICATION_DATE_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_MODIFICATION_DATE_FIELD = CommentConstants.COMMENT_MODIFICATION_DATE_FIELD;

    /**
     * Number of direct replies.
     * 
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_NUMBER_OF_REPLIES_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_NUMBER_OF_REPLIES = CommentConstants.COMMENT_NUMBER_OF_REPLIES_FIELD;

    /**
     * Last Reply Date in ISO-8601 representation.
     * 
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_LAST_REPLY_DATE_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_LAST_REPLY_DATE = CommentConstants.COMMENT_LAST_REPLY_DATE_FIELD;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link CommentConstants#COMMENT_PERMISSIONS_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String COMMENT_PERMISSIONS = CommentConstants.COMMENT_PERMISSIONS_FIELD;

    private CommentsConstants() {
    }

}

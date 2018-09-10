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
package org.nuxeo.ecm.platform.comment.workflow.utils;

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
     */
    public static final String COMMENT_DOC_TYPE = "Comment";

    /** @since 10.3 */
    public static final String COMMENT_SCHEMA = "comment";

    public static final String COMMENT_CREATION_DATE = "comment:creationDate";

    public static final String COMMENT_AUTHOR = "comment:author";

    public static final String COMMENT_TEXT = "comment:text";

    /** @since 10.3 */
    public static final String COMMENT_PARENT_ID = "comment:parentId";

    /** @since 10.3 */
    public static final String COMMENT_MODIFICATION_DATE = "comment:modificationDate";

    /** @since 10.3 */
    public static final String COMMENT_ANCESTOR_IDS = "comment:ancestorIds";

    // JSON constant part

    /** @since 10.3 */
    public static final String COMMENT_ENTITY_TYPE = "comment";

    /** @since 10.3 */
    public static final String COMMENTS_ENTITY_TYPE = "comments";

    /** @since 10.3 */
    public static final String COMMENT_ID_FIELD = "id";

    /** @since 10.3 */
    public static final String COMMENT_PARENT_ID_FIELD = "parentId";

    /** @since 10.3 */
    public static final String COMMENT_ANCESTOR_IDS_FIELD = "ancestorIds";

    /** @since 10.3 */
    public static final String COMMENT_AUTHOR_FIELD = "author";

    /** @since 10.3 */
    public static final String COMMENT_TEXT_FIELD = "text";

    /** @since 10.3 */
    public static final String COMMENT_CREATION_DATE_FIELD = "creationDate";

    /** @since 10.3 */
    public static final String COMMENT_MODIFICATION_DATE_FIELD = "modificationDate";

    private CommentsConstants() {
    }

}

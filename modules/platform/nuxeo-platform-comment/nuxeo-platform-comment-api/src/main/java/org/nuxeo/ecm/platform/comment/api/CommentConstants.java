/*
 * (C) Copyright 2007-2020 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.comment.api;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public final class CommentConstants {

    public static final String EVENT_COMMENT_CATEGORY = "commentCategory";

    /**
     * @since 11.1
     */
    public static final String TOP_LEVEL_DOCUMENT = "topLevelDocument";

    public static final String PARENT_COMMENT = "parentComment";

    /**
     * @since 11.1
     */
    public static final String PARENT_COMMENT_AUTHOR = "parentCommentAuthor";

    public static final String COMMENT_DOCUMENT = "comment_document";

    /** Key to reference comment text in properties' event. */
    public static final String COMMENT = "comment";

    /** Key to reference comment text in properties' event. */
    public static final String COMMENT_TEXT = "comment_text";

    public static final String COMMENT_TASK_TYPE = "comment_moderation";

    // --------------------------------------------
    // Document type, schema and property constants
    // --------------------------------------------

    /** @since 11.1 **/
    public static final String COMMENT_ROOT_DOC_TYPE = "CommentRoot";

    /** @since 11.1 */
    public static final String COMMENT_DOC_TYPE = "Comment";

    /** @since 11.1 */
    public static final String COMMENT_SCHEMA = "comment";

    /** @since 11.1 */
    public static final String COMMENT_CREATION_DATE_PROPERTY = "comment:creationDate";

    /** @since 11.1 */
    public static final String COMMENT_AUTHOR_PROPERTY = "comment:author";

    /** @since 11.1 */
    public static final String COMMENT_TEXT_PROPERTY = "comment:text";

    /** @since 11.1 */
    public static final String COMMENT_PARENT_ID_PROPERTY = "comment:parentId";

    /** @since 11.1 */
    public static final String COMMENT_MODIFICATION_DATE_PROPERTY = "comment:modificationDate";

    /** @since 11.1 */
    public static final String COMMENT_ANCESTOR_IDS_PROPERTY = "comment:ancestorIds";

    // -------------------------------------------
    // Entity type and field name constants (JSON)
    // -------------------------------------------

    /** @since 11.1 */
    public static final String COMMENT_ENTITY_TYPE = "comment";

    /** @since 11.1 */
    public static final String COMMENTS_ENTITY_TYPE = "comments";

    /** @since 11.1 */
    public static final String COMMENT_ID_FIELD = "id";

    /** @since 11.1 */
    public static final String COMMENT_PARENT_ID_FIELD = "parentId";

    /** @since 11.1 */
    public static final String COMMENT_ANCESTOR_IDS_FIELD = "ancestorIds";

    /** @since 11.1 */
    public static final String COMMENT_AUTHOR_FIELD = "author";

    /** @since 11.1 */
    public static final String COMMENT_TEXT_FIELD = "text";

    /**
     * Creation Date in ISO-8601 representation.
     *
     * @since 11.1
     */
    public static final String COMMENT_CREATION_DATE_FIELD = "creationDate";

    /**
     * Modification Date in ISO-8601 representation.
     *
     * @since 11.1
     */
    public static final String COMMENT_MODIFICATION_DATE_FIELD = "modificationDate";

    /**
     * Number of direct replies.
     *
     * @since 11.1
     */
    public static final String COMMENT_NUMBER_OF_REPLIES_FIELD = "numberOfReplies";

    /**
     * Last Reply Date in ISO-8601 representation.
     *
     * @since 11.1
     */
    public static final String COMMENT_LAST_REPLY_DATE_FIELD = "lastReplyDate";

    /** @since 11.1 */
    public static final String COMMENT_PERMISSIONS_FIELD = "permissions";

    // -------------------
    // Migration constants
    // -------------------

    /** @since 10.3 */
    public static final String MIGRATION_ID = "comment-storage"; // also in XML

    /** @since 10.3 */
    public static final String MIGRATION_STATE_RELATION = "relation"; // also in XML

    /** @since 10.3 */
    public static final String MIGRATION_STATE_PROPERTY = "property"; // also in XML

    /** @since 11.1 */
    public static final String MIGRATION_STATE_SECURED = "secured"; // also in XML

    /** @since 10.3 */
    public static final String MIGRATION_STEP_RELATION_TO_PROPERTY = "relation-to-property"; // also in XML

    /** @since 11.1 */
    public static final String MIGRATION_STEP_PROPERTY_TO_SECURED = "property-to-secured"; // also in XML

    private CommentConstants() {
        // utility class
    }

}

/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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

    public static final String COMMENT_DOCUMENT = "comment_document";

    public static final String COMMENT = "comment";

    public static final String COMMENT_TEXT = "comment_text";

    public static final String COMMENT_TASK_TYPE = "comment_moderation";

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
    }

}

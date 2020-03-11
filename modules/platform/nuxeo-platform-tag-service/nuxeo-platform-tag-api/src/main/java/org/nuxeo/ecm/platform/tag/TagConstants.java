/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Radu Darlea
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.tag;

/**
 * The tag constants.
 */
public class TagConstants {

    private TagConstants() {
    }

    public static final String TAG_DOCUMENT_TYPE = "Tag";

    public static final String TAG_LABEL_FIELD = "tag:label";

    public static final String TAGGING_DOCUMENT_TYPE = "Tagging";

    public static final String TAGGING_SOURCE_FIELD = "relation:source";

    public static final String TAGGING_TARGET_FIELD = "relation:target";

    /**
     * @since 9.3
     */
    public static final String TAG_LIST = "nxtag:tags";

    /**
     * @since 9.3
     */
    public static final String TAG_FACET = "NXTag";

    /**
     * @since 9.3
     */
    public static final String MIGRATION_ID = "tag-storage"; // also in XML

    /**
     * @since 9.3
     */
    public static final String MIGRATION_STATE_RELATIONS = "relations"; // also in XML

    /**
     * @since 9.3
     */
    public static final String MIGRATION_STATE_FACETS = "facets"; // also in XML

    /**
     * @since 9.3
     */
    public static final String MIGRATION_STEP_RELATIONS_TO_FACETS = "relations-to-facets"; // also in XML

}

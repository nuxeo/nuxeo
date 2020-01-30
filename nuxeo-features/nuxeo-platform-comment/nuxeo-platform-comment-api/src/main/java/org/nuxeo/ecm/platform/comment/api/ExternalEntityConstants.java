/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.comment.api;

/**
 * @since 10.3
 */
public class ExternalEntityConstants {

    private ExternalEntityConstants() {
        // utility class
    }

    // -------------------------------------------
    // Entity type and field name constants (JSON)
    // -------------------------------------------

    public static final String EXTERNAL_ENTITY = "entity";

    /** @since 11.1 */
    public static final String EXTERNAL_ENTITY_ID_FIELD = "entityId";

    /** @since 11.1 */
    public static final String EXTERNAL_ENTITY_ORIGIN_FIELD = "origin";

    /**
     * @deprecated since 11.1, use {@link ExternalEntityConstants#EXTERNAL_ENTITY_ID_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String EXTERNAL_ENTITY_ID = EXTERNAL_ENTITY_ID_FIELD;

    /**
     * @deprecated since 11.1, use {@link ExternalEntityConstants#EXTERNAL_ENTITY_ORIGIN_FIELD} instead
     */
    @Deprecated(since = "11.1")
    public static final String EXTERNAL_ENTITY_ORIGIN = EXTERNAL_ENTITY_ORIGIN_FIELD;

    // --------------------------------------------
    // Document type, schema and property constants
    // --------------------------------------------

    public static final String EXTERNAL_ENTITY_PROPERTY = "externalEntity:entity";

    public static final String EXTERNAL_ENTITY_ID_PROPERTY = "externalEntity:entityId";

    public static final String EXTERNAL_ENTITY_ORIGIN_PROPERTY = "externalEntity:origin";

    public static final String EXTERNAL_ENTITY_FACET = "ExternalEntity";

}

/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition;

/**
 * Constants used by the {@link org.nuxeo.ecm.platform.rendition.service.RenditionService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public class Constants {

    public static final String ALL_PUBLICATION_QUERY = "SELECT * FROM Document WHERE ecm:isProxy = 1 AND (rend:sourceVersionableId = %s OR ecm:proxyVersionableId = %s)";

    private Constants() {
        // Constants class
    }

    public static final String RENDITION_FACET = "Rendition";

    public static final String FILES_SCHEMA = "files";

    public static final String FILES_FILES_PROPERTY = "files:files";

    public static final String RENDITION_SCHEMA = "rendition";

    // version from which the rendition was derived (or live doc if not versionable)
    public static final String RENDITION_SOURCE_ID_PROPERTY = "rend:sourceId";

    // live doc if the rendition was derived from a versionable doc, otherwise null
    public static final String RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY = "rend:sourceVersionableId";

    // date the source doc was modified according to property named
    // RenditionDefinition#sourceDocumentModificationDatePropertyName
    public static final String RENDITION_SOURCE_MODIFICATION_DATE_PROPERTY = "rend:sourceModificationDate";

    public static final String RENDITION_NAME_PROPERTY = "rend:renditionName";

    /**
     * Rendition variant property name.
     *
     * @since 8.1
     */
    public static final String RENDITION_VARIANT_PROPERTY = "rend:renditionVariant";

    /**
     * Rendition variant property value prefix for a user.
     *
     * @since 8.1
     */
    public static final String RENDITION_VARIANT_PROPERTY_USER_PREFIX = "user:";

    /**
     * Rendition variant property value for an administrator.
     *
     * @since 8.1
     */
    public static final String RENDITION_VARIANT_PROPERTY_ADMINISTRATOR_USER = "administratoruser:";

    /**
     * @since 10.3
     */
    public static final String DEFAULT_RENDTION_PUBLISH_REASON = "publish";

}

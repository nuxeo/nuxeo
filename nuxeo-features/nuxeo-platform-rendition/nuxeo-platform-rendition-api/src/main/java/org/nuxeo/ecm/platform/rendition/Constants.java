/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public static final String RENDITION_NAME_PROPERTY = "rend:renditionName";
}

/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.picture.api;

/**
 * Imaging constants.
 *
 * @author Laurent Doguin
 */
public class ImagingDocumentConstants {

    public static final String PICTURE_TYPE_NAME = "Picture";

    public static final String PICTURE_FACET = "Picture";

    public static final String MULTIVIEW_PICTURE_FACET = "MultiviewPicture";

    public static final String PICTURE_SCHEMA_NAME = "picture";

    public static final String PICTUREBOOK_TYPE_NAME = "PictureBook";

    public static final String PICTURE_VIEWS_PROPERTY = PICTURE_SCHEMA_NAME + ":views";

    public static final String PICTURE_INFO_PROPERTY = PICTURE_SCHEMA_NAME + ":info";

    public static final String PICTURETEMPLATES_PROPERTY_NAME = "picturebook:picturetemplates";

    public static final String UPDATE_PICTURE_VIEW_EVENT = "updatePictureView";

    /**
     * @since 7.10
     */
    public static final String CTX_FORCE_VIEWS_GENERATION = "forceViewsGeneration";

    private ImagingDocumentConstants() {
        // Constants class
    }

}

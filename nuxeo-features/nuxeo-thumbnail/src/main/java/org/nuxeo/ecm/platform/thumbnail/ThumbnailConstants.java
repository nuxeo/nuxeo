/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Laurent Doguin <ldoguin@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.thumbnail;

/**
 * @since 5.7
 */
public class ThumbnailConstants {

    public static final String THUMBNAIL_FACET = "Thumbnail";

    public static final String THUMBNAIL_MIME_TYPE = "image/jpeg";

    public static final String THUMBNAIL_PROPERTY_NAME = "thumb:thumbnail";

    public static final String ANY_TO_THUMBNAIL_CONVERTER_NAME = "anyToThumbnail";

    public static final String PDF_AND_IMAGE_TO_THUMBNAIL_CONVERTER_NAME = "pdfAndImageToThumbnail";

    public static final String ANY_TO_PDF_TO_THUMBNAIL_CONVERTER_NAME = "anyToPdfToThumbnail";

    public static final String THUMBNAIL_SIZE_PARAMETER_NAME = "size";

    public static final String THUMBNAIL_DEFAULT_SIZE = "350x350";

    public enum EventNames {
        /**
         * Event sent after checking before updating document if the main blob has been updated
         */
        scheduleThumbnailUpdate // NOSONAR (matches the event name)
    }

}

/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    public static final String THUMBNAIL_CONVERTER_NAME = "thumbnailDocumentConverter";

    public static final String THUMBNAIL_SIZE_PARAMETER_NAME = "size";

    public static final String THUMBNAIL_DEFAULT_SIZE = "100";

    public enum EventNames {
        /**
         * Event sent after checking before updating document if the main blob
         * has been updated
         */
        afterBlobUpdateCheck,
    }

}

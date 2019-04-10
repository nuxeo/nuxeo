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
 *
 */
package org.nuxeo.ecm.platform.audio.extension;

/**
 * @since 5.7
 */
final class AudioThumbnailConstants {

    public static final String THUMBNAIL_FACET = "Thumbnail";

    public static final String THUMBNAIL_PROPERTY_NAME = "thumb:thumbnail";

    public static final String THUMBNAIL_CONVERTER_NAME = "thumbnailDocumentConverter";

    private AudioThumbnailConstants() {
        throw new AssertionError("Instantiating utility class...");

    }

}

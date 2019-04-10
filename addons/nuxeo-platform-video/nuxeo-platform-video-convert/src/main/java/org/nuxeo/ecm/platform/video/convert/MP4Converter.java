/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.convert;

/**
 * Convert to MP4 format.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class MP4Converter extends BaseVideoConversionConverter {

    public static final String MP4_VIDEO_MIMETYPE = "video/mp4";

    public static final String MP4_EXTENSION = ".mp4";

    public static final String TMP_DIRECTORY_PREFIX = "convertToMP4";

    @Override
    protected String getVideoMimeType() {
        return MP4_VIDEO_MIMETYPE;
    }

    @Override
    protected String getVideoExtension() {
        return MP4_EXTENSION;
    }

    @Override
    protected String getTmpDirectoryPrefix() {
        return TMP_DIRECTORY_PREFIX;
    }

}

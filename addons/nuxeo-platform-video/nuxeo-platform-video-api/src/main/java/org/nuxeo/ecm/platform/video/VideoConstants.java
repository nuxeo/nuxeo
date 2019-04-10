/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.video;

/**
 * Video constants.
 */
public class VideoConstants {

    public static final String VIDEO_TYPE = "Video";

    public static final String VIDEO_FACET = "Video";

    public static final String STORYBOARD_PROPERTY = "vid:storyboard";

    public static final String INFO_PROPERTY = "vid:info";

    public static final String DURATION_PROPERTY = "vid:info/duration";

    public static final String TRANSCODED_VIDEOS_PROPERTY = "vid:transcodedVideos";

    public static final String HAS_STORYBOARD_FACET = "HasStoryboard";

    public static final String HAS_VIDEO_PREVIEW_FACET = "HasVideoPreview";

    @Deprecated
    /**
     * @deprecated since 5.7.2
     */
    public static final String VIDEO_CHANGED_PROPERTY = "videoChanged";

    public static final String VIDEO_CHANGED_EVENT= "videoChanged";

    // Constant utility class.
    private VideoConstants() {
    }

}

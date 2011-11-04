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

package org.nuxeo.ecm.platform.video.service;

import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConversionStatus;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface VideoService {

    Collection<VideoConversion> getAvailableVideoConversions();

    void launchConversion(DocumentModel doc, String conversionName);

    void launchAutomaticConversions(DocumentModel doc);

    TranscodedVideo convert(Video originalVideo, String conversionName);

    TranscodedVideo convert(VideoConversionId id, Video originalVideo,
            String conversionName);

    VideoConversionStatus getProgressStatus(String repositoryName,
            DocumentRef docRef, String conversionName);

    void clearProgressStatus(VideoConversionId id);

}

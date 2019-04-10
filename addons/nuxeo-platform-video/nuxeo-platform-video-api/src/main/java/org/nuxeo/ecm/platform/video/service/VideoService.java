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
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConversionStatus;

/**
 * Service to asynchronously launch and monitor video conversions.
 * <p>
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface VideoService {

    /**
     * Returns the available registered video conversions that can be run on a
     * Video document.
     */
    Collection<VideoConversion> getAvailableVideoConversions();

    /**
     * Launch an asynchronously video conversion of the given {@code doc}.
     *
     * @param doc the video document to be converted
     * @param conversionName the video conversion to use
     */
    void launchConversion(DocumentModel doc, String conversionName);

    /**
     * Launch all the registered automatic video conversions on the given
     * {@code doc}.
     *
     * @param doc the video document to be converted
     */
    void launchAutomaticConversions(DocumentModel doc);

    /**
     * Convert the {@code originalVideo} using the given {@code conversionName}.
     *
     * @param originalVideo the video to convert
     * @param conversionName the video conversion to use
     * @return a {@code TranscodedVideo} object of the converted video.
     */
    TranscodedVideo convert(Video originalVideo, String conversionName);

    /**
     * Convert the {@code originalVideo} using the given {@code conversionName}.
     *
     * @param id unique identifier of the video conversion calling this method,
     *            used for monitoring.
     * @param originalVideo the video to convert
     * @param conversionName the video conversion to use
     * @return a {@code TranscodedVideo} object for the converted video.
     * @deprecated since 5.7.3, use the API without id
     * @see #convert(Video, String)
     */
    @Deprecated
    TranscodedVideo convert(VideoConversionId id, Video originalVideo,
            String conversionName);

    /**
     * Returns the status of the video conversion identified by the given
     * {@code id}.
     *
     * @param id unique identifier of the video conversion
     * @deprecated since 5.7.3, use the other API with a document
     * @see #getProgressStatus(String, String, String)
     */
    @Deprecated
    VideoConversionStatus getProgressStatus(VideoConversionId id);

    /**
     * Returns the status of the video conversion with the given conversion name
     * on the given document.
     *
     * @param repositoryName
     * @param docId
     * @param conversionName
     * @return
     * @since 5.7.3
     */
    VideoConversionStatus getProgressStatus(String repositoryName, String docId,
            String conversionName);

}

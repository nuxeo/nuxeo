/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Returns the available registered video conversions that can be run on a Video document.
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
     * Launch all the registered automatic video conversions on the given {@code doc}.
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
     * Returns the status of the video conversion with the given conversion name on the given document.
     *
     * @since 5.7.3
     */
    VideoConversionStatus getProgressStatus(String repositoryName, String docId, String conversionName);

    /**
     * @since 7.2
     */
    VideoConversion getVideoConversion(String conversionName);

    /**
     * @since 7.4
     */
    Configuration getConfiguration();

}

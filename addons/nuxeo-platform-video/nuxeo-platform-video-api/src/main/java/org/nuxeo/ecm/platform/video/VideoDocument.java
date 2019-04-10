/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.video;

import java.util.Collection;

/**
 * Interface for document adapter wrapping a Video document.
 * <p>
 * Gives access to the videos and related info stored in the underlying document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface VideoDocument {

    /**
     * Returns the main {@link Video} of this {@code VideoDocument}.
     */
    Video getVideo();

    /**
     * Returns all the {@link TranscodedVideo}s for this {@code VideoDocument}.
     */
    Collection<TranscodedVideo> getTranscodedVideos();

    /**
     * Returns a given {@link TranscodedVideo} based on its {@code name}, {@code null} if this transcoded video does not
     * exist.
     */
    TranscodedVideo getTranscodedVideo(String name);
}

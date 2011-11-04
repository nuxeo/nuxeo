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

package org.nuxeo.ecm.platform.video;

import java.util.Collection;

/**
 * Interface for document adapter wrapping a Video document.
 * <p>
 * Gives access to the videos and related info stored in the underlying
 * document.
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
     * Returns a given {@link TranscodedVideo} based on its {@code name},
     * {@code null} if this transcoded video does not exist.
     */
    TranscodedVideo getTranscodedVideo(String name);
}

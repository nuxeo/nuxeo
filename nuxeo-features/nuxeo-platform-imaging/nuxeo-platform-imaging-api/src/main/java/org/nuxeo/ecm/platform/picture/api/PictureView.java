/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.picture.api;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

public interface PictureView {

    String FIELD_TITLE = "title";

    String FIELD_DESCRIPTION = "description";

    String FIELD_TAG = "tag";

    String FIELD_WIDTH = "width";

    String FIELD_HEIGHT = "height";

    String FIELD_FILENAME = "filename";

    String FIELD_CONTENT = "content";

    String FIELD_INFO = "info";

    String getTitle();

    void setTitle(String title);

    String getTag();

    void setTag(String tag);

    String getDescription();

    void setDescription(String description);

    int getHeight();

    void setHeight(int height);

    int getWidth();

    void setWidth(int width);

    String getFilename();

    void setFilename(String filename);

    /**
     * Returns the {@code Blob} of the picture view.
     *
     * @deprecated since 7.2, use {@link #getBlob} instead
     */
    @Deprecated
    Blob getContent();

    /**
     * Returns the {@code ImageInfo} of the picture view.
     *
     * @since 7.1
     */
    ImageInfo getImageInfo();

    /**
     * Sets the {@code ImageInfo} of the picture view.
     *
     * @since 7.1
     */
    void setImageInfo(ImageInfo info);

    /**
     * Returns the {@code Blob} of the picture view.
     *
     * @since 5.7
     */
    Blob getBlob();

    /**
     * Sets the {@code Blob} of this picture view.
     *
     * @since 5.7
     */
    void setBlob(Blob blob);




    /**
     * Convert this {@code PictureView} as a MAp to be stored inside a document.
     *
     * @since 5.7
     */
    Map<String, Serializable> asMap();

}

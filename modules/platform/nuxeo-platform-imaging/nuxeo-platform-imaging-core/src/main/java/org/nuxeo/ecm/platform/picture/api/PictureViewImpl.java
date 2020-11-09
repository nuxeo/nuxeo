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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

public class PictureViewImpl implements PictureView {

    int width;

    int height;

    String title;

    String description;

    String tag;

    String filename;

    Blob content;

    Blob blob;

    ImageInfo imageInfo;

    /**
     * @since 5.7
     */
    public PictureViewImpl() {
    }

    /**
     * @since 5.7
     */
    public PictureViewImpl(Map<String, Serializable> m) {
        title = (String) m.get(PictureView.FIELD_TITLE);
        description = (String) m.get(PictureView.FIELD_DESCRIPTION);
        tag = (String) m.get(PictureView.FIELD_TAG);
        filename = (String) m.get(PictureView.FIELD_FILENAME);
        blob = (Blob) m.get(PictureView.FIELD_CONTENT);
        imageInfo = (ImageInfo) m.get(PictureView.FIELD_INFO);

        Integer w = (Integer) m.get(PictureView.FIELD_WIDTH);
        if (w != null) {
            width = w;
        }
        Integer h = (Integer) m.get(PictureView.FIELD_HEIGHT);
        if (h != null) {
            height = h;
        }

    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public Blob getContent() {
        return blob;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public Blob getBlob() {
        return blob;
    }

    @Override
    public void setBlob(Blob blob) {
        this.blob = blob;
    }

    @Override
    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    @Override
    public void setImageInfo(ImageInfo info) {
        this.imageInfo = info;
    }

    @Override
    public Map<String, Serializable> asMap() {
        Map<String, Serializable> m = new HashMap<>();
        m.put(PictureView.FIELD_TITLE, getTitle());
        m.put(PictureView.FIELD_DESCRIPTION, getDescription());
        m.put(PictureView.FIELD_TAG, getTag());
        m.put(PictureView.FIELD_HEIGHT, getHeight());
        m.put(PictureView.FIELD_WIDTH, getWidth());
        m.put(PictureView.FIELD_FILENAME, getFilename());
        m.put(PictureView.FIELD_CONTENT, (Serializable) blob);
        m.put(PictureView.FIELD_INFO, (Serializable) imageInfo.toMap());
        return m;
    }
}

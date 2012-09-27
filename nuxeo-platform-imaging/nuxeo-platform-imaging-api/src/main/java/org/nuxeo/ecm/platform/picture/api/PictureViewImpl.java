/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    Object content;

    Blob blob;

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
    public Object getContent() {
        return content;
    }

    @Override
    public void setContent(Object content) {
        this.content = content;
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
    public Map<String, Serializable> asMap() {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(PictureView.FIELD_TITLE, getTitle());
        m.put(PictureView.FIELD_DESCRIPTION, getDescription());
        m.put(PictureView.FIELD_TAG, getTag());
        m.put(PictureView.FIELD_HEIGHT, getHeight());
        m.put(PictureView.FIELD_WIDTH, getWidth());
        m.put(PictureView.FIELD_FILENAME, getFilename());
        m.put(PictureView.FIELD_CONTENT, (Serializable) blob);
        return m;
    }
}

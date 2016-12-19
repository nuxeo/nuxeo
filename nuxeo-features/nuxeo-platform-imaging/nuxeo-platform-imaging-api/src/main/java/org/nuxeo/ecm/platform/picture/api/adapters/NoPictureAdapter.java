/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.api.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;

/**
 * Picture adapter that creates no picture views at all.
 */
public class NoPictureAdapter extends AbstractPictureAdapter {

    public static final String ORIGINAL_VIEW_NAME = "Original";

    @Override
    public void doRotate(int angle) {
    }

    @Override
    public void doCrop(String coords) {
    }

    @Override
    public Blob getPictureFromTitle(String title) throws PropertyException {
        if (ORIGINAL_VIEW_NAME.equals(title)) {
            return (Blob) doc.getPropertyValue("file:content");
        }
        return null;
    }

    @Override
    public String getFirstViewXPath() {
        return "file:"; // "content" added by caller
    }

    @Override
    public String getViewXPath(String viewName) {
        if (ORIGINAL_VIEW_NAME.equals(viewName)) {
            return getFirstViewXPath();
        }
        return null;
    }

    @Override
    public boolean fillPictureViews(Blob blob, String filename, String title,
            List<Map<String, Object>> pictureConversions) {
        return true;
    }

    @Override
    public void preFillPictureViews(Blob blob, List<Map<String, Object>> pictureConversions, ImageInfo imageInfo) {
    }

}

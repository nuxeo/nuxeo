/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.api.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;

/**
 * Picture adapter that creates no picture views at all.
 */
public class NoPictureAdapter extends AbstractPictureAdapter {

    public static final String ORIGINAL_VIEW_NAME = "Original";

    @Override
    public boolean createPicture(Blob blob, String filename, String title,
            ArrayList<Map<String, Object>> pictureTemplates)
            throws IOException, ClientException {
        // create no views
        return true;
    }

    @Override
    public void doRotate(int angle) {
    }

    @Override
    public void doCrop(String coords) {
    }

    @Override
    public Blob getPictureFromTitle(String title) throws PropertyException,
            ClientException {
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
            ArrayList<Map<String, Object>> pictureTemplates) {
        return true;
    }

    @Override
    public void preFillPictureViews(Blob blob,
            List<Map<String, Object>> pictureTemplates, ImageInfo imageInfo) {
    }

}

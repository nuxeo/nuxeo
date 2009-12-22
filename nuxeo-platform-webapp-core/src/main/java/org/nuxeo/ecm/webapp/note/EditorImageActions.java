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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.note;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Provides actions related to inserting an image for Note documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public interface EditorImageActions {

    String getSelectedTab();

    // image uploading related methods
    void setUploadedImage(InputStream uploadedImage);

    InputStream getUploadedImage();

    void setUploadedImageName(String uploadedImageName);

    String getUploadedImageName();

    String uploadImage() throws ClientException;

    boolean getIsImageUploaded();

    boolean getInCreationMode();

    String getUrlForImage();

    // image searching related methods
    String searchImages() throws ClientException;

    List<DocumentModel> getSearchImageResults();

    boolean getHasSearchResults();

    String getSearchKeywords();

    void setSearchKeywords(final String searchKeywords);

    List<Map<String, String>> getSizes();

    void setSelectedSize(final String selectedSize);

    String getSelectedSize();

    String getImageProperty();

    void destroy();

}

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

package org.nuxeo.ecm.platform.picture.web;

import java.io.IOException;
import java.util.ArrayList;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * Provides picture-related actions.
 *
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 */
public interface PictureManager {

    /**
     * Creates and saves a picture document.
     * @deprecated since 5.5
     */
    @Deprecated
    String addPicture() throws Exception;

    String crop() throws ClientException, IOException;

    /**
     * Turns every view of a picture 90 degrees to the left.
     */
    String rotate90left() throws ClientException, IOException;

    /**
     * Turns every view of a picture 90 degrees to the right.
     */
    String rotate90right() throws ClientException, IOException;

    void download(DocumentView docView) throws ClientException;

    /**
     * Gets the content of the Picture. It's the uploaded file.
     *
     * @return a Blob holding the uploaded file
     */
    Blob getFileContent();

    /**
     * Sets the content of the Picture. It's the uploaded file.
     *
     * @param fileContent a Blob holding the uploaded file
     */
    void setFileContent(Blob fileContent);

    /**
     * Gets the filename of the uploaded file.
     *
     * @return a String holding the filename.
     */
    String getFilename();

    /**
     * Sets the filename of the uploaded file.
     *
     * @param filename a String holding the filename.
     */
    void setFilename(String filename);

    /**
     * Gets the fileurl. FileUrl is used to create valid link expression for the
     * download function from the index of the picture's views.
     *
     * @return a String holding the fileurl.
     */
    String getFileurlPicture() throws ClientException;

    /**
     * Sets the fileurl. FileUrl is used to create valid link expression for the
     * download function from the index of the picture's views.
     *
     * @param fileurlPicture a String holding the fileurl.
     */
    void setFileurlPicture(String fileurlPicture);

    /**
     * Gets the index. This index is used to display the selected picture in
     * view_picture.
     *
     * @return an Integer holding the index.
     */
    Integer getIndex();

    /**
     * Sets the index. This index is used to display the selected picture in
     * view_picture.
     *
     * @param index an Integer holding the index.
     */
    void setIndex(Integer index);

    /**
     * Sets the selectedItems. This array contains an index and the title of
     * each picture's view. It's used to dynamically the selected view.
     *
     * @param selectItems an Array holding the selectItems.
     */
    void setSelectItems(ArrayList selectItems);

    /**
     * Gets the selectedItems. This array contains an index and the title of
     * each picture's view. It's used to dynamically the selected view.
     *
     * @return an Array holding the selectItems.
     */
    ArrayList getSelectItems() throws ClientException;

    String getCropCoords();

    void setCropCoords(String cropCoords);

    /**
     * Listener reinitializing values at every Document changes.
     */
    void resetFields();

    void initialize() throws Exception;

    void destroy();

}

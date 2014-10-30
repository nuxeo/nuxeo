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

package org.nuxeo.ecm.webapp.filemanager;

import java.io.InputStream;

import javax.faces.event.ActionEvent;

import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.api.SimpleFileManager;

public interface FileManageActions extends SimpleFileManager {

    /**
     * @since 6.0
     */
    public static final String NUXEO_JSF_TMP_DIR_PROP = "nuxeo.jsf.tmp.dir";

    String display();

    /**
     * Adds a new File.
     *
     * @return the page that displays the documents
     */
    String addFile() throws ClientException;

    void setFileUpload(InputStream fileUpload);

    InputStream getFileUpload();

    void setFileName(String fileName);

    String getFileName();

    /**
     * Setter to get the filename to remove, works in conjunction with
     * {@link #removeOneOrAllUploadedFiles(ActionEvent)}.
     *
     * @since 5.9.2
     */
    void setFileToRemove(String fileToRemove);

    /**
     * Removes one of all uploaded files, depending on previous call to
     * {@link #setFileToRemove(String)}.
     * <p>
     * This is useful to remove files in an Ajax context to avoid Seam
     * remoting, and still get the selected entry from JavaScript variables
     * (see NXP-13234).
     *
     * @since 5.9.2
     * @throws ClientException
     */
    String removeOneOrAllUploadedFiles(ActionEvent action)
            throws ClientException;

    @WebRemote
    String addFolderFromPlugin(String fullName, String morePath)
            throws ClientException;

    @WebRemote
    String addFileFromPlugin(String content, String mimetype, String fullName,
            String morePath, Boolean UseBase64) throws ClientException;

    @WebRemote
    boolean canWrite() throws ClientException;

    @WebRemote
    String moveWithId(String docId, String containerId) throws ClientException;

    @WebRemote
    String copyWithId(String docId) throws ClientException;

    @WebRemote
    String pasteWithId(String docId) throws ClientException;

    @WebRemote
    String removeUploadedFile(String fileName) throws ClientException;

    @WebRemote
    String removeAllUploadedFile() throws ClientException;

    @WebRemote
    String removeSingleUploadedFile() throws ClientException;

}

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

package org.nuxeo.ecm.webapp.filemanager;

import java.io.InputStream;

import javax.faces.event.ActionEvent;

import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.platform.ui.web.api.SimpleFileManager;

public interface FileManageActions extends SimpleFileManager {

    /**
     * @since 6.0
     */
    String NUXEO_JSF_TMP_DIR_PROP = "nuxeo.jsf.tmp.dir";

    @Override
    String display();

    /**
     * Adds a new File.
     *
     * @return the page that displays the documents
     */
    String addFile();

    void setFileUpload(InputStream fileUpload);

    InputStream getFileUpload();

    void setFileName(String fileName);

    String getFileName();

    /**
     * Setter to get the filename to remove, works in conjunction with {@link #removeOneOrAllUploadedFiles(ActionEvent)}
     * .
     *
     * @since 5.9.2
     */
    void setFileToRemove(String fileToRemove);

    /**
     * Removes one of all uploaded files, depending on previous call to {@link #setFileToRemove(String)}.
     * <p>
     * This is useful to remove files in an Ajax context to avoid Seam remoting, and still get the selected entry from
     * JavaScript variables (see NXP-13234).
     *
     * @since 5.9.2
     */
    String removeOneOrAllUploadedFiles(ActionEvent action);

    @WebRemote
    String addFolderFromPlugin(String fullName, String morePath);

    @WebRemote
    String addFileFromPlugin(String content, String mimetype, String fullName, String morePath, Boolean UseBase64);

    @WebRemote
    boolean canWrite();

    @WebRemote
    String moveWithId(String docId, String containerId);

    @WebRemote
    String copyWithId(String docId);

    @WebRemote
    String pasteWithId(String docId);

    @WebRemote
    String removeUploadedFile(String fileName);

    @WebRemote
    String removeAllUploadedFile();

    @WebRemote
    String removeSingleUploadedFile();

}

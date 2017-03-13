/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.filemanager.api;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * File Manager.
 * <p>
 * File Manager to handle file
 *
 * @author Andreas Kalogeropoulos
 */
public interface FileManager {

    /**
     * Returns an initialized doc based on a given blob.
     *
     * @param input the blob containing the content and the mime type
     * @param path the path were to create the document
     * @param overwrite whether to overwrite an existing file with the same title or not
     * @param fullName the fullname that contains the filename
     * @return the created Document
     */
    DocumentModel createDocumentFromBlob(CoreSession documentManager, Blob input, String path, boolean overwrite,
            String fullName) throws IOException;

    /**
     * Just applies the same actions as creation but does not changes the doc type.
     *
     * @param input the blob containing the content and the mime type
     * @param path the path to the file to update
     * @param fullName the full name that contains the filename
     * @return the updated Document
     */
    DocumentModel updateDocumentFromBlob(CoreSession documentManager, Blob input, String path, String fullName);

    /**
     * Creates a Folder.
     *
     * @param fullname the full name of the folder
     * @param path the path were to create the folder
     * @return the Folder Created
     * @deprecated since 9.1, use {@link #createFolder(CoreSession, String, String, boolean)} instead
     */
    @Deprecated
    default DocumentModel createFolder(CoreSession documentManager, String fullname, String path) throws IOException {
        return createFolder(documentManager, fullname, path, true);
    }

    /**
     * Creates a Folder.
     *
     * @param fullname the full name of the folder
     * @param path the path were to create the folder
     * @param overwrite whether to overwrite an existing folder with the same title or not
     * @return the Folder Created
     * @since 9.1
     */
    DocumentModel createFolder(CoreSession documentManager, String fullname, String path, boolean overwrite)
            throws IOException;

    /**
     * Returns the list of document that are to be suggested to principalName as a candidate container for a new
     * document of type docType on all registered repositories.
     *
     * @return the list of candidate containers
     */
    DocumentModelList getCreationContainers(Principal principal, String docType);

    /**
     * Returns the list of document that are to be suggested to the principal of documentManager as a candidate
     * container for a new document of type docType.
     *
     * @return the list of candidate containers
     */
    DocumentModelList getCreationContainers(CoreSession documentManager, String docType);

    List<DocumentLocation> findExistingDocumentWithFile(CoreSession documentManager, String path, String digest,
            Principal principal);

    boolean isUnicityEnabled();

    List<String> getFields();

    String getDigestAlgorithm();

    boolean isDigestComputingEnabled();

    /**
     * Gets the versioning applied on an overwritten document before it is overwritten.
     *
     * @since 5.7
     */
    VersioningOption getVersioningOption();

    /**
     * Checks whether versioning should also be applied after a document is added.
     *
     * @since 5.7
     */
    boolean doVersioningAfterAdd();

}

/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
     * Returns an initialized doc based on a given blob.
     *
     * @param input the blob containing the content and the mime type
     * @param path the path were to create the document
     * @param overwrite whether to overwrite an existing file with the same title or not
     * @param fullName the fullname that contains the filename
     * @param noMimeTypeCheck true if the blob's mime-type doesn't have to be checked against fullName
     * @return the created Document
     * @since 8.10
     */
    DocumentModel createDocumentFromBlob(CoreSession documentManager, Blob input, String path, boolean overwrite,
            String fullName, boolean noMimeTypeCheck) throws IOException;

    /**
     * Returns an initialized doc based on a given blob.
     *
     * @param input the blob containing the content and the mime type
     * @param path the path were to create the document
     * @param overwrite whether to overwrite an existing file with the same title or not
     * @param fullName the fullname that contains the filename
     * @param noMimeTypeCheck true if the blob's mime-type doesn't have to be checked against fullName
     * @param excludeOneToMany true if the importers creating more than one document for the given blob must be excluded
     *            when selecting the importer
     * @return the created Document
     * @since 10.3
     */
    DocumentModel createDocumentFromBlob(CoreSession documentManager, Blob input, String path, boolean overwrite,
            String fullName, boolean noMimeTypeCheck, boolean excludeOneToMany) throws IOException;

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
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Deprecated
    VersioningOption getVersioningOption();

    /**
     * Checks whether versioning should also be applied after a document is added.
     *
     * @since 5.7
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Deprecated
    boolean doVersioningAfterAdd();

}

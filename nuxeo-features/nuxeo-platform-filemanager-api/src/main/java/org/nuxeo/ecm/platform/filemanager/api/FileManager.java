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
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;

/**
* File Manager.
* <p>
* File Manager to handle file
*
* @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
*         Kalogeropoulos</a>
*/
public interface FileManager {

   /**
    * Returns an initialized doc based on a given file.
    *
    * @param file
    *            the uploading file
    * @param path
    *            the path were to create the document
    * @param overwrite
    *            boolean how decide to overwrite or not
    * @return the created Document
    */
//   DocumentModel createDocumentFromFile(CoreSession documentManager,
//           UploadedFile file, String path, boolean overwrite) throws Exception;

   /**
    * Returns an initialized doc based on a given blob.
    *
    * @return the created Document
    * @param input
    *            the blob containing the content and the mime/type
    * @param path
    *            the path were to create the document
    * @param overwrite
    *            boolean how decide to overwrite or not
    * @param fullName
    *            the fullname that containes the filename
    */
   DocumentModel createDocumentFromBlob(CoreSession documentManager,
           Blob input, String path, boolean overwrite, String fullName)
           throws Exception;


   /**
    * Just applies the same actions as creation but does not changes the doc
    * type.
    *
    * @param input
    *            the blob containing the content and the mime/type
    * @param path
    *            the path to the file to update
    * @param fullName
    *            the fullname that containes the filename
    * @return the updated Document
    */
   DocumentModel updateDocumentFromBlob(CoreSession documentManager,
           Blob input, String path, String fullName) throws Exception;

   /**
    * Creates a Folder.
    *
    * @return teh Folder Created
    * @param fullname
    *            teh full Name of the folder
    * @param path
    *            the path were to create the folder
    */
   DocumentModel createFolder(CoreSession documentManager,
           String fullname, String path) throws Exception;

    /**
     * Return the list of document that are to be suggested to principalName as
     * a candidate container for a new document of type docType on all
     * registered repositories.
     *
     * @param principal
     * @param docType
     * @return the list of candidate containers
     * @throws Exception
     */
    DocumentModelList getCreationContainers(Principal principal, String docType)
            throws Exception;

    /**
     * Return the list of document that are to be suggested to the principal of
     * documentManager as a candidate container for a new document of type
     * docType.
     *
     * @param documentManager
     * @param docType
     * @return the list of candidate containers
     * @throws Exception
     */
    DocumentModelList getCreationContainers(CoreSession documentManager, String docType)
            throws Exception;

   String computeDigest(Blob blob) throws ClientException, NoSuchAlgorithmException, IOException;


   List<DocumentLocation> findExistingDocumentWithFile(String path,
           String digest, Principal principal) throws ClientException, SearchException, QueryException;

   List<DocumentLocation> findExistingDocumentWithFile(String path,
           Blob blob, Principal principal) throws ClientException;

   boolean  isFileAlreadyPresentInPath(String path, String digest,
            Principal principal) throws ClientException, SearchException, QueryException;

   boolean isFileAlreadyPresentInPath(String path, Blob blob,
                   Principal principal) throws ClientException;

   public boolean isUnicityEnabled() throws ClientException;


   public List<String> getFields() throws ClientException;

   public String getDigestAlgorithm();

   public boolean isDigestComputingEnabled();
}

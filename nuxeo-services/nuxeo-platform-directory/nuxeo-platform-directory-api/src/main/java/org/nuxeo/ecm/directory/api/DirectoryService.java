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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.api;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.model.ComponentName;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public interface DirectoryService {

    ComponentName NAME = new ComponentName("org.nuxeo.ecm.directory.DirectoryServiceImpl");

    List<String> getDirectoryNames();

    String getDirectorySchema(String directoryName) throws DirectoryException;

    /**
     * Opens a session on specified directory.
     * <p>
     * This method prefers to throw rather than returning null.
     *
     * @param directoryName
     * @return the session
     * @throws DirectoryException in case the session cannot be created
     */
    Session open(String directoryName) throws DirectoryException;

    /**
     * Opens a session on the directory for the specified context. The context is given by the document. The document
     * service will try to find the directory local configuration of the document that will specify the suffix. the
     * directory will fetch the directoryName + suffix found. If no local configuration is found the service will return
     * the directoryName directory.
     * <p>
     * This method prefers to throw rather than returning null.
     *
     * @param directoryName
     * @param documentContext
     * @return the session
     * @throws DirectoryException in case the session cannot be created
     */
    Session open(String directoryName, DocumentModel documentContext) throws DirectoryException;

    /**
     * Return the directory for the specified context. The context is given by the document. The document service will
     * try to find the directory local configuration of the document that will specify the suffix. The directory service
     * will fetch the directoryName + suffix found. If no local configuration is found the service will return the
     * directoryName directory.
     * <p>
     * If the directoryName is null, return null.
     *
     * @param directoryName
     * @param documentContext
     * @return the directory, if the factory of the directory or the directory itself is not found return null
     * @throws DirectoryException
     */
    Directory getDirectory(String directoryName, DocumentModel documentContext) throws DirectoryException;

    /**
     * Return the directory with the name directoryName.
     * <p>
     * If the directoryName is null, return null.
     *
     * @param directoryName
     * @return the directory, if the factory of the directory or the directory itself is not found return null
     * @throws DirectoryException
     */
    Directory getDirectory(String directoryName) throws DirectoryException;

    /**
     * Return all the directories registered into the service.
     * <p>
     *
     * @throws DirectoryException
     */
    List<Directory> getDirectories() throws DirectoryException;

    void registerDirectory(String directoryName, DirectoryFactory factory);

    void unregisterDirectory(String directoryName, DirectoryFactory factory);

    String getDirectoryIdField(String directoryName) throws DirectoryException;

    String getDirectoryPasswordField(String directoryName) throws DirectoryException;

    /**
     * Returns the name of the parent directory of specified directory, if applicable.
     *
     * @param directoryName
     * @return the name, or null
     * @throws DirectoryException
     */
    String getParentDirectoryName(String directoryName) throws DirectoryException;

}

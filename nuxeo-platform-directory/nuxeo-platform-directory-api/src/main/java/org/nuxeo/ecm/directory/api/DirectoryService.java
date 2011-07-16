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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.model.ComponentName;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public interface DirectoryService {

    ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.directory.DirectoryServiceImpl");

    List<String> getDirectoryNames() throws ClientException;

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
     * Opens a session on the directory for the specified context. The context
     * is given by the document. The document service will try to find the
     * directory local configuration of the document that will specify the
     * suffix. the directory will fetch the directoryName + suffix found. If no
     * local configuration is found the service will return the directoryName
     * directory.
     * <p>
     * This method prefers to throw rather than returning null.
     *
     * @param directoryName
     * @param documentContext
     * @return the session
     * @throws DirectoryException in case the session cannot be created
     */
    Session open(String directoryName, DocumentModel documentContext)
            throws DirectoryException;

    /**
     * Return the directory for the specified context. The
     * context is given by the document. The document service will try to find
     * the directory local configuration of the document that will specify the
     * suffix. The directory service will fetch the directoryName + suffix
     * found. If no local configuration is found the service will return the
     * directoryName directory.
     * <p>
     * If the directoryName is null, return null.
     *
     * @param directoryName
     * @param documentContext
     * @return the directory, if the factory of the directory or the directory
     *         itself is not found return null
     * @throws DirectoryException
     */
    Directory getDirectory(String directoryName, DocumentModel documentContext)
            throws DirectoryException;

    /**
     * Return the directory with the name directoryName.
     * <p>
     * If the directoryName is null, return null.
     *
     * @param directoryName
     * @return the directory, if the factory of the directory or the directory
     *         itself is not found return null
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

    String getDirectoryPasswordField(String directoryName)
            throws DirectoryException;

    /**
     * Returns the name of the parent directory of specified directory, if
     * applicable.
     *
     * @param directoryName
     * @return the name, or null
     * @throws DirectoryException
     */
    String getParentDirectoryName(String directoryName)
            throws DirectoryException;

}

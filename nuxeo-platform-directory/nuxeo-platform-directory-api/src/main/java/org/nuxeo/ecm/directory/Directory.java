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
 * $Id$
 */

package org.nuxeo.ecm.directory;

import java.util.Collection;

/**
 * The directory interface.
 * <p>
 * This interface is implemented in order to create an NXDirectory. One should
 * implement this interface in order to create either a new Directory
 * implementation or a new Directory Source.
 *
 * @author glefter@nuxeo.com
 */
// TODO: maybe separate Directory implementation and Directory Source
public interface Directory {

    /**
     * Gets the unique name of the directory, used for registering.
     *
     * @return the unique directory name
     * @throws DirectoryException
     */
    String getName() throws DirectoryException;

    /**
     * Gets the schema name used by this directory.
     *
     * @return the schema name
     * @throws DirectoryException
     */
    String getSchema() throws DirectoryException;

    /**
     * Gets the name of the parent directory. This is used for hierarchical
     * vocabularies.
     *
     * @return the name of the parent directory, or null.
     */
    String getParentDirectory() throws DirectoryException;

    /**
     * Gets the id field of the schema for this directory.
     *
     * @return the id field.
     * @throws DirectoryException
     */
    String getIdField() throws DirectoryException;

    /**
     * Gets the password field of the schema for this directory.
     *
     * @return the password field.
     * @throws DirectoryException
     */
    String getPasswordField() throws DirectoryException;

    /**
     * Gets the ID generator used when creating new entries in this directory.
     *
     * @return an ID Generator object
     * @throws DirectoryException
     */
    IdGenerator getIdGenerator() throws DirectoryException;

    /**
     * Shuts down the directory.
     *
     * @throws DirectoryException
     */
    void shutdown() throws DirectoryException;

    /**
     * Creates a session for accessing entries in this directory.
     *
     * @return a Session object
     * @throws DirectoryException if a session cannot be created
     */
    Session getSession() throws DirectoryException;

    /**
     * Lookup a Reference by field name.
     *
     * @return the matching reference implementation or null
     * @throws DirectoryException
     */
    Reference getReference(String referenceFieldName) throws DirectoryException;

    /**
     * Lookup all References defined on the directory.
     *
     * @return all registered references
     * @throws DirectoryException
     */
    Collection<Reference> getReferences() throws DirectoryException;

    /**
     * Gets the cache instance of the directory
     *
     * @return the cache of the directory
     * @throws DirectoryException
     */
    DirectoryCache getCache() throws DirectoryException;

    /**
     * Invalidates the cache instance of the directory
     *
     * @throws DirectoryException
     */
    void invalidateDirectoryCache() throws DirectoryException;


}

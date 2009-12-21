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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.SizeLimitExceededException;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public interface DirectoryManager extends DirectoryService {

    /**
     * Retrieves a directory entry using its id.
     * <p>
     * TODO what happens when the entry is not found? return null if not found?
     *
     * @param id the entry id
     * @return a DocumentModel representing the entry
     * @throws DirectoryException
     */
    DocumentModel getEntry(long sessionId, String id) throws DirectoryException;

    /**
     * Retrieves a directory entry using its id.
     *
     * @see #getEntry(long, String)
     *
     * @param id the entry id
     * @param fetchReferences boolean stating if references have to be fetched
     * @return a DocumentModel representing the entry
     * @throws DirectoryException
     */
    DocumentModel getEntry(long sessionId, String id, boolean fetchReferences)
            throws DirectoryException;

    /**
     * Retrieves all the entries in the directory.
     *
     * @return a collection with all the entries in the directory
     * @throws DirectoryException
     * @throws SizeLimitExceededException if the number of results is larger
     *             than the limit configured for the directory
     */
    DocumentModelList getEntries(long sessionId) throws DirectoryException;

    /**
     * Creates an entry in a directory.
     *
     * @param fieldMap A map with keys and values that should be stored in a
     *            directory
     *            <p>
     *            Note: The values in the map should be of type String
     * @return The new entry created in the directory
     * @throws UnsupportedOperationException if the directory does not allow the
     *             creation of new entries
     * @throws DirectoryException if a communication exception occurs or if an
     *             entry with the same id already exists.
     */
    DocumentModel createEntry(long sessionId, Map<String, Object> fieldMap)
            throws DirectoryException;

    /**
     * Updates a directory entry.
     *
     * @param docModel The entry to update
     * @throws UnsupportedOperationException if the directory does not support
     *             entry updating
     * @throws DirectoryException if a communication error occurs
     */
    void updateEntry(long sessionId, DocumentModel docModel)
            throws DirectoryException;

    /**
     * Deletes a directory entry.
     *
     * @param docModel The entry to delete
     * @throws UnsupportedOperationException if the directory does not support
     *             entry deleting
     * @throws DirectoryException if a communication error occurs
     */
    void deleteEntry(long sessionId, DocumentModel docModel)
            throws DirectoryException;

    /**
     * Deletes a directory entry by id.
     *
     * @param id the id of the entry to delete
     * @throws UnsupportedOperationException if the directory does not support
     *             entry deleting
     * @throws DirectoryException if a communication error occurs
     */
    void deleteEntry(long sessionId, String id) throws DirectoryException;

    /**
     * Deletes a directory entry by id and secondary ids.
     * <p>
     * This is used for hierarchical vocabularies, where the actual unique key
     * is the couple (parent, id).
     *
     * @param sessionId the session id.
     * @param id the id of the entry to delete.
     * @param map a map of seconday key values.
     * @throws DirectoryException if a communication error occurs.
     */
    void deleteEntry(long sessionId, String id, Map<String, String> map)
            throws DirectoryException;

    /*
     * FIXME: Parses a query string and create a query object for this
     * directory.
     *
     * @param query the query string to parse @return a new query object @throws
     * QueryException if the query cannot be parsed
     *
     * maybe not needed public SQLQuery createQuery(String query) throws
     * QueryException;
     */

    /**
     * Executes a simple query. The conditions will be 'AND'-ed. Search is done
     * with exact match.
     *
     * @param filter a filter to apply to entries in directory
     * @return a list of document models containing the entries matched by the
     *         query
     * @throws DirectoryException if a communication error occurs
     * @throws SizeLimitExceededException if the number of results is larger
     *             than the limit configured for the directory
     */
    DocumentModelList query(long sessionId, Map<String, Serializable> filter)
            throws DirectoryException;

    /**
     * Executes a simple query. The conditions will be 'AND'-ed.
     * <p>
     * fieldNames present in the fulltext set are treated as a fulltext match.
     *
     * @param filter a filter to apply to entries in directory
     * @param fulltext a set of field that should be treated as a fulltext
     *            search
     * @return a list of document models containing the entries matched by the
     *         query
     * @throws DirectoryException if a communication error occurs
     * @throws SizeLimitExceededException if the number of results is larger
     *             than the limit configured for the directory
     */
    DocumentModelList query(long sessionId, Map<String, Serializable> filter,
            Set<String> fulltext) throws DirectoryException;

    /**
     * Executes a simple query. The conditions will be 'AND'-ed and the result
     * will be sorted by the orderBy criteria list.
     * <p>
     * fieldNames present in the fulltext set are treated as a fulltext match.
     *
     * @param filter a filter to apply to entries in directory
     * @param orderBy a LinkedHashMap with the 'order by' criterias.The key of
     *            an entry of this map represents the column name and the value
     *            of the same entry represent the column order,which may be
     *            'asc' or 'desc'.
     * @param fulltext a set of field that should be treated as a fulltext
     *            search
     * @return a list of document models containing the entries matched by the
     *         query
     * @throws DirectoryException if a communication error occurs
     * @throws SizeLimitExceededException if the number of results is larger
     *             than the limit configured for the directory
     */
    DocumentModelList query(long sessionId, Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws DirectoryException;

    DocumentModelList query(long sessionId, Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) throws DirectoryException;

    // TODO: create an API to allow sql AND/OR/NOT/LIKE conditions
    // public DocumentModelList query(Criteria criteria ) throws
    // DirectoryException;

    /**
     * Commits any changes on this session.
     *
     * @throws UnsupportedOperationException if the directory does not support
     *             transactions
     * @throws DirectoryException if a communication error occurs
     *             <p>
     *             In this case the session will be automatically rollbacked
     */
    void commit(long sessionId) throws DirectoryException;

    /**
     * Rollbacks any changes on this session.
     *
     * @throws UnsupportedOperationException if the associated directory does
     *             not support transactions
     * @throws DirectoryException if a communication error occurs
     *             <p>
     *             In this case, the session will be automatically rollbacked
     *             anyway
     */
    void rollback(long sessionId) throws DirectoryException;

    /**
     * Closes the session and all open result sets obtained from this session.
     * <p>
     * Releases this Connection object's resources immediately instead of
     * waiting for them to be automatically released.
     * <p>
     * TODO: should this operation auto-commit pending changes?
     *
     * @throws DirectoryException if a communication error occurs
     */
    void close(long sessionId) throws DirectoryException;

    /**
     * Executes a query using filter and return only the column <b>columnName</b>.
     *
     * @param filter the filter for the query
     * @param columnName the column whose content should be returned
     * @return the list with the values of <b>columnName</b> for the entries
     *         matching <b>filter</b>
     * @throws DirectoryException
     * @throws SizeLimitExceededException if the number of results is larger
     *             than the limit configured for the directory
     */
    List<String> getProjection(long sessionId, Map<String, Serializable> filter,
            String columnName) throws DirectoryException;

    List<String> getProjection(long sessionId, Map<String, Serializable> filter,
            Set<String> fulltext, String columnName) throws DirectoryException;

    /**
     * Tells whether the directory implementation can be used as an
     * authenticating backend for the UserManager (based on login / password
     * check).
     *
     * @return true is the directory is authentication aware
     * @throws DirectoryException
     */
    boolean isAuthenticating(long sessionId) throws DirectoryException;

    /**
     * Checks that the credentials provided by the UserManager match those
     * registered in the directory. If username is not in the directory, this
     * should return false instead of throrwing an exception.
     *
     * @param username
     * @param password
     * @return true is the credentials match those stored in the directory
     * @throws DirectoryException
     */
    boolean authenticate(long sessionId, String username, String password)
            throws DirectoryException;

    /**
     * The Id field is the name of the field that is used a primary key: unique
     * and not null value in the whole directory. This field is also used as
     * login field if the directory is authenticating.
     *
     * @return the name of the id field
     * @throws DirectoryException
     */
    String getIdField(long sessionId) throws DirectoryException;

    /**
     * @return the name of the field to store the password if the directory is
     *         authenticating (can be null)
     * @throws DirectoryException
     */
    String getPasswordField(long sessionId) throws DirectoryException;

    boolean isReadOnly(long sessionId) throws DirectoryException;

    /**
     * Returns true if session has an entry with given id.
     *
     * @since 5.2M4
     * @throws DirectoryException
     */
    boolean hasEntry(long sessionId, String id) throws DirectoryException;

    /**
     * Creates an entry in a directory.
     *
     * @since 5.2M4
     * @param entry the document model representing the entry to create
     * @return The new entry created in the directory
     * @throws UnsupportedOperationException if the directory does not allow the
     *             creation of new entries
     * @throws DirectoryException if a communication exception occurs or if an
     *             entry with the same id already exists.
     */
    DocumentModel createEntry(long sessionId, DocumentModel entry)
            throws DirectoryException;

}

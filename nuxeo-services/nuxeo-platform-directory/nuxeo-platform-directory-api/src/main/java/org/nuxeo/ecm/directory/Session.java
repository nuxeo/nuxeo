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
 * $Id$
 */

package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * A session used to access entries in a directory.
 * <p>
 * This class is used to create, obtain, modify and delete entries in a directory.
 *
 * @see
 * @see Directory#getSession()
 * @author glefter@nuxeo.com
 */

public interface Session extends AutoCloseable {

    /**
     * Retrieves a directory entry using its id.
     * <p>
     * TODO what happens when the entry is not found? return null if not found?
     *
     * @param id the entry id
     * @return a DocumentModel representing the entry
     */
    DocumentModel getEntry(String id);

    /**
     * Retrieves a directory entry using its id.
     *
     * @param id the entry id
     * @param fetchReferences boolean stating if references have to be fetched
     * @return a DocumentModel representing the entry
     */
    DocumentModel getEntry(String id, boolean fetchReferences);

    /**
     * Retrieves all the entries in the directory. If the remote server issues a size limit exceeded error while sending
     * partial results up to that limit, the method {@code DocumentModelList#totalsize} on the returned list will return
     * -2 as a special marker for truncated results.
     *
     * @deprecated since 6.0 Use query method instead with parameters
     * @return a collection with all the entries in the directory
     * @throws SizeLimitExceededException if the number of results is larger than the limit configured for the directory
     *             and the server does not send partial results.
     */
    @Deprecated
    DocumentModelList getEntries();

    /**
     * Creates an entry in a directory.
     *
     * @param fieldMap A map with keys and values that should be stored in a directory
     *            <p>
     *            Note: The values in the map should be of type String
     * @return The new entry created in the directory
     * @throws UnsupportedOperationException if the directory does not allow the creation of new entries
     */
    DocumentModel createEntry(Map<String, Object> fieldMap);

    /**
     * Updates a directory entry.
     *
     * @param docModel The entry to update
     * @throws UnsupportedOperationException if the directory does not support entry updating
     */
    void updateEntry(DocumentModel docModel);

    /**
     * Deletes a directory entry.
     *
     * @param docModel The entry to delete
     * @throws UnsupportedOperationException if the directory does not support entry deleting
     */
    void deleteEntry(DocumentModel docModel);

    /**
     * Deletes a directory entry by id.
     *
     * @param id the id of the entry to delete
     * @throws UnsupportedOperationException if the directory does not support entry deleting
     */
    void deleteEntry(String id);

    /**
     * Deletes a directory entry by id and secondary ids.
     * <p>
     * This is used for hierarchical vocabularies, where the actual unique key is the couple (parent, id).
     *
     * @param id the id of the entry to delete.
     * @param map a map of secondary key values.
     * @deprecated since 9.2 (unused), use {@link #deleteEntry(String)} instead.
     */
    @Deprecated
    void deleteEntry(String id, Map<String, String> map);

    /*
     * FIXME: Parses a query string and create a query object for this directory.
     * @param query the query string to parse @return a new query object @throws QueryException if the query cannot be
     * parsed maybe not needed public SQLQuery createQuery(String query) throws QueryException;
     */

    /**
     * Executes a simple query. The conditions will be 'AND'-ed. Search is done with exact match.
     * <p>
     * Does not fetch reference fields.
     * </p>
     * If the remote server issues a size limit exceeded error while sending partial results up to that limit, the
     * method {@code DocumentModelList#totalsize} on the returned list will return -2 as a special marker for truncated
     * results.
     *
     * @param filter a filter to apply to entries in directory
     * @return a list of document models containing the entries matched by the query
     * @throws SizeLimitExceededException if the number of results is larger than the limit configured for the directory
     *             and the server does not send partial results.
     */
    DocumentModelList query(Map<String, Serializable> filter);

    /**
     * Executes a simple query. The conditions will be 'AND'-ed.
     * <p>
     * fieldNames present in the fulltext set are treated as a fulltext match. Does not fetch reference fields.
     * </p>
     * If the remote server issues a size limit exceeded error while sending partial results up to that limit, the
     * method {@code DocumentModelList#totalsize} on the returned list will return -2 as a special marker for truncated
     * results.
     *
     * @param filter a filter to apply to entries in directory
     * @param fulltext a set of field that should be treated as a fulltext search
     * @return a list of document models containing the entries matched by the query
     * @throws SizeLimitExceededException if the number of results is larger than the limit configured for the directory
     *             and the server does not send partial results.
     */
    DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext);

    /**
     * Executes a simple query. The conditions will be 'AND'-ed and the result will be sorted by the orderBy criteria
     * list.
     * <p>
     * fieldNames present in the fulltext set are treated as a fulltext match. Does not fetch reference fields.
     * </p>
     * If the remote server issues a size limit exceeded error while sending partial results up to that limit, the
     * method {@code DocumentModelList#totalsize} on the returned list will return -2 as a special marker for truncated
     * results.
     *
     * @param filter a filter to apply to entries in directory
     * @param orderBy a LinkedHashMap with the 'order by' criterias.The key of an entry of this map represents the
     *            column name and the value of the same entry represent the column order,which may be 'asc' or 'desc'.
     * @param fulltext a set of field that should be treated as a fulltext search
     * @return a list of document models containing the entries matched by the query
     * @throws SizeLimitExceededException if the number of results is larger than the limit configured for the directory
     *             and the server does not send partial results.
     */
    DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy);

    /**
     * Executes a query with the possibility to fetch references
     *
     * @see #query(Map, Set, Map)
     */
    DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences);

    /**
     * Executes a query with the possibility to fetch a subset of the results. org.nuxeo.ecm.directory.BaseSession
     * provides a default implementation fetching all results to return the subset. Not recommended.
     *
     * @param limit maximum number of results ignored if less than 1
     * @param offset number of rows skipped before starting, will be 0 if less than 0.
     * @see #query(Map, Set, Map, boolean)
     * @since 5.7
     */
    DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset);

    // TODO: create an API to allow sql AND/OR/NOT/LIKE conditions
    // public DocumentModelList query(Criteria criteria )

    /**
     * Closes the session and all open result sets obtained from this session.
     * <p>
     * Releases this Connection object's resources immediately instead of waiting for them to be automatically released.
     * <p>
     * TODO: should this operation auto-commit pending changes?
     */
    @Override
    void close();

    /**
     * Executes a query using filter and return only the column <b>columnName</b>.
     *
     * @param filter the filter for the query
     * @param columnName the column whose content should be returned
     * @return the list with the values of <b>columnName</b> for the entries matching <b>filter</b>
     * @throws SizeLimitExceededException if the number of results is larger than the limit configured for the directory
     */
    List<String> getProjection(Map<String, Serializable> filter, String columnName);

    List<String> getProjection(Map<String, Serializable> filter, Set<String> fulltext, String columnName);

    /**
     * Tells whether the directory implementation can be used as an authenticating backend for the UserManager (based on
     * login / password check).
     *
     * @return true is the directory is authentication aware
     */
    boolean isAuthenticating();

    /**
     * Checks that the credentials provided by the UserManager match those registered in the directory. If username is
     * not in the directory, this should return false instead of throrwing an exception.
     *
     * @param username
     * @param password
     * @return true is the credentials match those stored in the directory
     */
    boolean authenticate(String username, String password);

    /**
     * The Id field is the name of the field that is used a primary key: unique and not null value in the whole
     * directory. This field is also used as login field if the directory is authenticating.
     *
     * @return the name of the id field
     */
    String getIdField();

    /**
     * @return the name of the field to store the password if the directory is authenticating (can be null)
     */
    String getPasswordField();

    boolean isReadOnly();

    /**
     * Returns true if session has an entry with given id.
     *
     * @since 5.2M4
     */
    boolean hasEntry(String id);

    /**
     * Creates an entry in a directory.
     *
     * @since 5.2M4
     * @param entry the document model representing the entry to create
     * @return The new entry created in the directory
     * @throws UnsupportedOperationException if the directory does not allow the creation of new entries
     */
    DocumentModel createEntry(DocumentModel entry);

    /**
     * For test framework. Changes the read/query methods to return all of the entries, including the password field.
     *
     * @param readAllColumns whether to read all columns
     * @since 9.1
     */
    void setReadAllColumns(boolean readAllColumns);

}

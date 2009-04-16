/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id:SearchEngineBackend.java 13117 2007-03-01 17:43:28Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.backend;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;

/**
 * Search engine backend interface.
 *
 * <p>
 *
 * You must implement this interface to register a new backend implementation
 * against the search service.
 *
 * </p>
 *
 * @see org.nuxeo.ecm.core.search.api.backend.impl.AbstractSearchEngineBackend
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface SearchEngineBackend extends Serializable {

    /**
     * Returns the plugin name.
     *
     * @returns the pluginQueryException name
     */
    String getName();

    /**
     * Set the name of the plugin.
     *
     * @param name : the name of the plugin.
     */
    void setName(String name);

    /**
     * Get configuration file name if any.
     *
     * <p>
     * Expected to be loadable in the classpath.
     * </p>
     *
     * <p>
     * This aimed at being optional. See backend descriptor in core.
     * </p>
     *
     * @return the configuration file name.
     */
    String getConfigurationFileName();

    /**
     * Set the configuration filr name for this backend.
     *
     * <p>
     * This aimed at being optional. See backend descriptor in core.
     * </p>
     *
     * @param configurationFileName : the name of the configuration file with
     *            extension.
     */
    void setConfigurationFileName(String configurationFileName);

    /**
     * Index a set of resources.
     *
     * @param resources : ResolvedResources instance.
     * @throws IndexingException
     */
    void index(ResolvedResources resources) throws IndexingException;

    /**
     * Deletes an index given an aggregated resources key
     *
     * <p>
     * This will remove <strong>all</strong> resources indexed with <param>key</param>
     * as key used to identified the set ot resources. See
     * <code>ResolvedResources.getId()</code>
     * </p>
     *
     * @param key : aggregated resources key.
     * @throws IndexingException
     */
    void deleteAggregatedResources(String key) throws IndexingException;

    /**
     * Deletes an atomic resource given its key.
     *
     * <p>
     * This will remove the resource identified by this resource key
     * <strong>only</strong>.
     * </p>
     *
     * @param key : atomic resource key
     * @throws IndexingException
     */
    void deleteAtomicResource(String key) throws IndexingException;

    /**
     * Clear all the indexes.
     *
     * @throws IndexingException
     */
    void clear() throws IndexingException;

    /**
     * Searches results given an NXQL query.
     *
     * @param nxqlQuery : a native NXP query
     * @param offset pagination start
     * @param range number of results.
     * @return a result set instance
     * @throws SearchException if an error occured while performing the search
     */
    ResultSet searchQuery(ComposedNXQuery nxqlQuery, int offset, int range)
            throws SearchException;

    /**
     * Searches results given a backened specific native query string.
     *
     * @param queryString : a backened specific native query string wrapper
     * @param offset pagination start
     * @param range pagination stop
     * @return a result set instance
     * @throws SearchException
     * @throws QueryException
     */
    ResultSet searchQuery(NativeQueryString queryString, int offset, int range)
            throws SearchException, QueryException;

    /**
     * Searches results given a native query.
     *
     * @param nativeQuery : a backened specific native query wrapper.
     * @param offset pagination start
     * @param range pagination stop
     * @return a result set instance
     * @throws SearchException if an error occured while performing the search
     * @throws QueryException if the query is invalid or unsupported
     */
    ResultSet searchQuery(NativeQuery nativeQuery, int offset, int range)
            throws SearchException, QueryException;

    /**
     * Returns the supported analyers.
     *
     * @return a list of identifiers.
     */
    List<String> getSupportedAnalyzersFor();

    /**
     * Returns the supported field types.
     *
     * @return a list of identifiers.
     */
    List<String> getSupportedFieldTypes();

    /**
     * Opens a new session.
     *
     * @return a new backend session id
     */
    SearchServiceSession createSession();

    /**
     * Closes a search service session given its session id.
     *
     */
    void closeSession(String sid);

    /**
     * Save all pending sessions.
     *
     * @throws IndexingException
     */
    void saveAllSessions() throws IndexingException;

}

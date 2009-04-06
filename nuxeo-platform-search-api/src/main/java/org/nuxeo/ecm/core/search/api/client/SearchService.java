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
 * $Id: SearchService.java 29869 2008-02-02 09:12:31Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.client;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.indexing.blobs.BlobExtractor;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.events.IndexingEventConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocType;

/**
 * Search service public interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface SearchService extends Serializable {

    /**
     * Opens a new session against the search service.
     * <p>
     * Warning: for now on the client is responsible for closing the session.
     *
     * @return a search service session.
     */
    SearchServiceSession openSession();

    /**
     * Closes a search service session given its identifier.
     *
     * @param sid the search service identifier
     */
    void closeSession(String sid);

    /**
     * Saves all the pending sessions.
     * <p>
     * This is useful when using document batch size greater than 1 since if the
     * amount of documents indexed are not an exact multiple of the document
     * batch size then one might want to flush right now on demande the remaning
     * resources in sessions before the next window size is reached.
     *
     * @throws IndexingException
     */
    void saveAllSessions() throws IndexingException;

    /**
     * Is the search service enabled?
     * <p>
     * It is possible to disable the search service using extension point parameter.
     *
     * @return true if active / false if inactive.
     */
    boolean isEnabled();

    /**
     * Sets the status of the search service.
     *
     * @param active if active is true, then the service will be activated
     */
    void setStatus(boolean active);

    /**
     * Adds / updates index(es) given an <code>IndexableResources</code>
     * instance.
     * <p>
     * The actual resolution in this case will be done search service side.
     *
     * @param sources an IndexableResources instance
     * @param fulltext do compute fulltext at resolution time
     * @throws IndexingException wrap low level backend exception
     */
    void index(IndexableResources sources, boolean fulltext)
            throws IndexingException;

    /**
     * Adds / updates index(es) given (<code>ResolvedResources</code>
     * <p>
     * This method is useful for performing the resource resolution outside the
     * search service.
     *
     * @param sources resolved resources
     * @throws IndexingException wrap low level backend exception
     */
    void index(ResolvedResources sources) throws IndexingException;

    void unindex(DocumentModel dm) throws IndexingException;

    /**
     * Completely erases the indexes.
     *
     * @throws IndexingException
     */
    void clear() throws IndexingException;

    /**
     * Deletes an index given an aggregated resources key.
     * <p>
     * This will remove <strong>all</strong> resources indexed with
     * <param>key</param> as key used to identified the set ot resources. See
     * <code>ResolvedResources.getId()</code>
     *
     * @param key aggregated resources key.
     * @throws IndexingException
     */
    void deleteAggregatedResources(String key) throws IndexingException;

    /**
     * Deletes an atomic resource given its key.
     * <p>
     * This will remove the resource identified by this resource key
     * <strong>only</strong>.
     *
     * @param key atomic resource key
     * @throws IndexingException
     */
    void deleteAtomicResource(String key) throws IndexingException;

    /**
     * Searches results given an NXQL query.
     *
     * @param nxqlQuery a NXQL query instance
     * @param offset pagination start
     * @param range number of results
     *
     * @return a ResultSet instance
     * @throws SearchException wrap low level backend exception
     * @throws QueryException if the query is invalid or unsupported
     * @deprecated use {@link CoreSession#query} instead
     */
    @Deprecated
    ResultSet searchQuery(ComposedNXQuery nxqlQuery, int offset, int range)
            throws SearchException, QueryException;

    /**
     * Searches results given a backend native query wrapper.
     *
     * @param nativeQuery the backend native query wrapper.
     * @param offset pagination start
     * @param range number of results
     *
     * @return a result set instance
     * @throws SearchException wrap low level backend exception
     * @throws QueryException if the query is invalid or unsupported
     * @deprecated use {@link CoreSession#query} instead
     */
    @Deprecated
    ResultSet searchQuery(NativeQuery nativeQuery, int offset, int range)
            throws SearchException, QueryException;

    /**
     * Searches results given a backend native query string.
     *
     * @param queryString the backend native query string wrapper.
     * @param backendName the backened name to apply the query on. If backened
     *            name is null then use the default indexing backends
     * @param offset pagination start
     * @param range number of results
     *
     * @return a result set instance
     * @throws SearchException wrap low level backend exception
     * @throws QueryException wrong query, rewrapped from backend
     * @deprecated use {@link CoreSession#query} instead
     */
    @Deprecated
    ResultSet searchQuery(NativeQueryString queryString, String backendName,
            int offset, int range) throws SearchException, QueryException;

    /**
     * Returns the supported analyers for a given backend.
     *
     * @param backendName the backend name.
     * @return a list of identifiers.
     */
    List<String> getSupportedAnalyzersFor(String backendName);

    /**
     * Returns the supported fieldd types for a given backend.
     *
     * @param backendName the backend name.
     * @return a list of identifiers.
     */
    List<String> getSupportedFieldTypes(String backendName);

    /**
     * Return the indexing information for a given nuxeo core doc type.
     *
     * @param docType doc type indentifier.
     * @return an indexable doc type instance.
     */
    IndexableDocType getIndexableDocTypeFor(String docType);

    /**
     * Returns an indexable resource configuration given its name.
     *
     * @param name the name of the indexable resource configuration.
     * @param full compute automatic configuration to get the full resource
     *            configuration
     * @return an indexable resource configuration instance given its name.
     */
    IndexableResourceConf getIndexableResourceConfByName(String name,
            boolean full);

    /**
     * Returns an indexable resource configuration given its prefix.
     *
     * @param prefix the prefix of the indexable resource configuration.
     * @param full compute automatic configuration to get the full resource
     *            configuration
     * @return an indexable resource configuration instance given its name.
     */
    IndexableResourceConf getIndexableResourceConfByPrefix(String prefix,
            boolean full);

    /**
     * Returns all the indexable resource configurations registred.
     *
     * @return the a map from indexable resource configuration name to indexable
     *         resource configuration instance.
     */
    Map<String, IndexableResourceConf> getIndexableResourceConfs();

    /**
     * Returns the list of all backend names.
     *
     * @return the list of all backend names.
     */
    String[] getAvailableBackendNames();

    /**
     * Computes a search principal out from a principal instance.
     * <p>
     * NuxeoPrincipal instance is expected for groups support.
     *
     * @param principal a java principal instance
     * @return a search principal instance
     */
    SearchPrincipal getSearchPrincipal(Principal principal);

    /**
     * Returns the full text descriptor given its name.
     *
     * @param prefixedName the prefixed name with what it's been registered
     *            using extension point.
     * @return a full text field descriptor.
     */
    FulltextFieldDescriptor getFullTextDescriptorByName(String prefixedName);

    /**
     * Returns an indexing event configuration given its name.
     *
     * @param name the name under which it's been registered using extension
     *            point.
     * @return the configuration object.
     */
    IndexingEventConf getIndexingEventConfByName(String name);

    /**
     * Invalidates the computed indexable resource confs.
     * <p>
     * Will be useful if Nuxeo Runtime supports hot deployment in the future.
     */
    void invalidateComputedIndexableResourceConfs();

    /**
     * Returns a blob extractor given its name.
     *
     * @param name the name against which the full text extractor has been
     *            registered using extension point.
     * @return
     */
    BlobExtractor getBlobExtractorByName(String name);

    /**
     * Returns a resource type descriptor instance.
     *
     * @param name the resource type name which has been used with extension
     *            point.
     * @return a resource type descriptor instance
     */
    ResourceTypeDescriptor getResourceTypeDescriptorByName(String name);

    /**
     * Returns the amount of threads the search service will be able to
     * instanciate within its thread pool.
     *
     * @return the number of threads max
     */
    int getNumberOfIndexingThreads();

    /**
     * Returns the number of indexing tasks waiting for a slot in the ThreadPool executor.
     *
     * @return
     */
    long getIndexingWaitingQueueSize();

    /**
     * Sets the amount of threads the search service will be able to instanciate
     * within its thread pool.
     *
     * @param numberOfIndexingThreads : the number of threads max
     */
    void setNumberOfIndexingThreads(int numberOfIndexingThreads);

    /**
     * Returns the document batch size.
     * <p>
     * In case of batch indexing we can choose to save the indexing session for
     * a given amount of document. Default is one meaning the session is saved
     * after every insertion. You might want to increase this while performing
     * bulk document imports.
     *
     * @return number of of document per indexing session before save()
     */
    int getIndexingDocBatchSize();

    /**
     * Sets the document batch size.
     * <p>
     * In case of batch indexing we can choose to save the indexing session for
     * a given amount of document. Default is one meaning the session is saved
     * after every insertion. You might want to increase this while performing
     * bulk document imports.
     *
     * @param docBatchSize number of of document per indexing session before
     *            save()
     */
    void setIndexingDocBatchSize(int docBatchSize);

    /**
     * Performs a full reindexing of the Nuxeo core repository given a Nuxeo core
     * repository name and a path.
     * <p>
     * If path is null then the reindexing will be done from the root of the
     * repository (i.e : "/").
     *
     * @param repoName the Nuxeo Core repository name.
     * @param fulltext whether or not we want to index fulltext.
     * @parem path the path from which the reindexing will occur.
     * @throws IndexingException
     * @deprecated does nothing
     */
    @Deprecated
    void reindexAll(String repoName, String path, boolean fulltext)
            throws IndexingException;

    /**
     * Is the search service reindexing the while indexes?
     *
     * @return a flag
     */
    boolean isReindexingAll();

    /**
     * Reindex all setter.
     *
     * @param flag bool flag
     */
    void setReindexingAll(boolean flag);

    /**
     * Returns the number of running indexing tasks.
     *
     * @return the number of actively running indexing tasks
     */
    int getActiveIndexingTasks();

    /**
     * Returns the total number of completed indexing tasks.
     * <p>
     * If you want to use this API for monitoring purpose, you should be aware
     * that the total number of indexing tasks is reinitialized when the
     * component is loaded only. Thus you should keep track of this value
     * <strong>before</strong> performing your indexing operation you want to
     * keep track of.
     *
     * @return the total number of commpleted indexing tasks.
     */
    long getTotalCompletedIndexingTasks();

}

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
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.search.backend.compass;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.indexing.blobs.BlobExtractor;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.events.IndexingEventConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocType;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;

/**
 * A SearchServiceInternals implementation to be used in unit tests.
 *
 * <p>
 * The configuration is a simple public hashmap, so that test cases can fill it.
 * The keys have to be the fully qualified field names, as used in queries, e.g,
 *
 * <pre>
 * dc:title
 * </pre>
 *
 * </p>
 *
 * TODO Move (as a backend-agnostic helper) in
 * org.nuxeo.ecm.core.search.testing?
 *
 * @author gracinet
 *
 */
@SuppressWarnings("serial")
public class FakeSearchService implements SearchServiceInternals {

    public final Map<String, FakeIndexableResourceDataDescriptor> dataConfs;

    public final Map<String, Set<String>> facetsToTypes;

    public final Map<String, Set<String>> facetsCollectionToTypes;

    public final Map<String, Set<String>> typeInheritance;

    public FakeSearchService() {
        dataConfs = new HashMap<String, FakeIndexableResourceDataDescriptor>();
        facetsToTypes = new HashMap<String, Set<String>>();
        facetsCollectionToTypes = new HashMap<String, Set<String>>();
        typeInheritance = new HashMap<String, Set<String>>();
    }

    public IndexableResourceDataConf getIndexableDataConfByName(String name,
            boolean full) {
        return dataConfs.get(name);
    }

    public IndexableResourceDataConf getIndexableDataConfFor(String dataName) {
        return dataConfs.get(dataName);
    }

    // Unused methods

    public String getDefaultSearchEngineBakendName() {
        return null;
    }

    public Map<String, IndexableDocType> getIndexableDocTypes() {
        return null;
    }

    public String getPreferedBackendNameFor(ResolvedResource resource) {
        return null;
    }

    public SearchEngineBackend getSearchEngineBackendByName(String name) {
        return null;
    }

    public Map<String, SearchEngineBackend> getSearchEngineBackends() {
        return null;
    }

    public void setDefaultSearchEngineBackendName(String backendName) {
    }

    public void deleteAggregatedResources(String key) throws IndexingException {
    }

    public void deleteAtomicResource(String key) throws IndexingException {
    }

    public String[] getAvailableBackendNames() {
        return null;
    }

    public IndexableResourceConf getIndexableResourceConfByName(String name,
            boolean full) {
        return null;
    }

    public Map<String, IndexableResourceConf> getIndexableResourceConfs() {
        return null;
    }

    public IndexableDocType getIndexableDocTypeFor(String docType) {
        return null;
    }

    public List<String> getSupportedAnalyzersFor(String backendName) {
        return null;
    }

    public List<String> getSupportedFieldTypes(String backendName) {
        return null;
    }

    public void index(IndexableResources sources) throws IndexingException {
    }

    public ResultSet searchQuery(ComposedNXQuery nxqlQuery, int offset,
            int range) throws SearchException {
        return null;
    }

    public ResultSet searchQuery(NativeQuery nativeQuery, int offset, int range)
            throws SearchException {
        return null;
    }

    public ResultSet searchQuery(NativeQueryString queryString,
            String backendName, int offset, int range) throws SearchException {
        return null;
    }

    public SearchPrincipal getSearchPrincipal(Principal principal) {
        return null;
    }

    public IndexableResourceConf getIndexableResourceConfByPrefix(
            String prefix, boolean full) {
        return null;
    }

    public boolean isEnabled() {
        return false;
    }

    public void setStatus(boolean active) {
    }

    public FulltextFieldDescriptor getFullTextDescriptorByName(String name) {
        FulltextFieldDescriptor desc = null;
        if (BuiltinDocumentFields.FIELD_FULLTEXT.equals(name)) {
            desc = new FulltextFieldDescriptor();
            desc.setAnalyzer("lowerWhitespace");
            desc.setName(name);
        }
        return desc;
    }

    public Set<String> getDocumentTypeNamesForFacet(String facet) {
        return facetsToTypes.get(facet);
    }

    public Set<String> getDocumentTypeNamesForFacet(Collection<String> facets) {
        // avoid some oddities with Collection objects as keys in maps
        String[] fl = new String[facets.size()];
        int i = 0;
        for (String f : facets) {
            fl[i++] = f;
        }
        Arrays.sort(fl);
        return facetsCollectionToTypes.get(Arrays.asList(fl).toString());
    }

    public IndexableResourceDataConf getIndexableDataConfByName(String name) {
        return null;
    }

    public void invalidateComputedIndexableResourceConfs() {
    }

    public void clear() throws IndexingException {
    }

    public Set<String> getDocumentTypeNamesExtending(String docType) {
        return typeInheritance.get(docType);
    }

    public IndexingEventConf getIndexingEventConfByName(String name) {
        return null;
    }

    public BlobExtractor getBlobExtractorByName(String name) {
        return null;
    }

    public boolean hasAsynchronousIndexing() {
        return false;
    }

    public ResourceTypeDescriptor getResourceTypeDescriptorByName(String name) {
        return null;
    }

    public void index(ResolvedResources sources) throws IndexingException {
    }

    public void index(IndexableResources sources, boolean fulltext)
            throws IndexingException {
    }

    public long getIndexingWaitingQueueSize() {
        return 0;
    }

    public int getNumberOfIndexingThreads() {
        return 0;
    }

    public void indexInThread(DocumentModel dm, Boolean recursive,
            boolean fulltext) {
    }

    public void closeSession(String sid) {
    }

    public SearchServiceSession openSession() {
        return null;
    }

    public int getIndexingDocBatchSize() {
        return 1;
    }

    public void setIndexingDocBatchSize(int docBatchSize) {
    }

    public void setNumberOfIndexingThreads(int numberOfIndexingThreads) {
    }

    public void saveAllSessions() throws IndexingException {
    }

    public void reindexAll(String repoName, String path, boolean fulltext)
            throws IndexingException {
    }

    public int getActiveIndexingTasks() {
        return 0;
    }

    public long getTotalCompletedIndexingTasks() {
        return 0;
    }

    public void indexInThread(ResolvedResources sources)
            throws IndexingException {
    }

    public boolean isReindexingAll() {
        return false;
    }

    public void setReindexingAll(boolean flag) {
    }

    public void index(IndexableResources sources, boolean fulltext,
            boolean newTxn) throws IndexingException {
    }

    public void index(ResolvedResources sources, boolean newTxn)
            throws IndexingException {
    }

    public void clear(boolean newTxn) throws IndexingException {
    }

}

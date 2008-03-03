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
 *     anguenot
 *
 * $Id: IndexingDocumentModelPrefetchedListener.java 29569 2008-01-23 14:42:42Z tdelprat $
 */

package org.nuxeo.ecm.platform.search.core.listener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedDataImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourceImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourcesImpl;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.common.TypeManagerServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentIndexableResourceImpl;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourcesFactory;
import org.nuxeo.ecm.core.search.api.events.IndexingEventConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.ecm.platform.events.api.EventMessage;

/**
 * Indexing document model prefetched core event istener.
 *
 * <p>
 * Core event listener that takes care of document model prefetched data
 * indexing. This listener is useful to index in a synchronous way minimum
 * document model data so that document updates can show up directly after one
 * request
 * </p>
 *
 * <p>
 * Perform and generate partial schema resolved resource generation in a
 * programatic way. Usually, this is performed search service side using XML
 * based contributions. The prefetch data are the minimum data needed for the
 * search based virtualn navigation tree can be displayed. As well, it will
 * compute search engine mandatory builtins.
 * </p>
 *
 * <p>
 * The rest of the indexing will be done in an async way by a message driven
 * bean.
 * </p>
 *
 * @see org.nuxeo.ecm.platform.search.ejb.SearchMessageListener
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class IndexingDocumentModelPrefetchedListener extends
        AbstractEventListener {

    private static final Log log = LogFactory.getLog(IndexingDocumentModelPrefetchedListener.class);

    private static final List<String> eventsToIgnore = new ArrayList<String>();

    private static SearchService searchService;

    private static SchemaManager typeManager;

    private static SearchService getSearchService() {
        if (searchService == null) {
            searchService = SearchServiceDelegate.getRemoteSearchService();
        }
        return searchService;
    }

    private static SchemaManager getTypeManager() {
        if (typeManager == null) {
            typeManager = TypeManagerServiceDelegate.getRemoteTypeManagerService();
        }
        return typeManager;
    }

    public void notifyEvent(CoreEvent event) throws Exception {

        String eventId = event.getEventId();
        Object ob = event.getSource();

        if (!(ob instanceof DocumentModel)) {
            return;
        }

        DocumentModel dm = (DocumentModel) ob;

        if (dm.getContextData(EventMessage.BLOCK_SYNC_INDEXING) != null
                && (Boolean) dm.getContextData(EventMessage.BLOCK_SYNC_INDEXING) == true) {
            log.debug("sync indexing is blocked for doc " + dm.getRef().toString());
            return;
        }

        SearchService service = getSearchService();
        // Check if the search service is active
        if (!service.isEnabled()) {
            log.debug("search service not enabled");
            return;
        }

        // Check if event need to be ignored
        if (eventsToIgnore != null && eventsToIgnore.contains(eventId)) {
            log.debug("not interested about event with id=" + eventId);
            return;
        }
        IndexingEventConf eventConf = service.getIndexingEventConfByName(eventId);
        if (eventConf == null) {
            log.debug("not interested about event with id=" + eventId);
            eventsToIgnore.add(eventId);
            return;
        }

        String action = eventConf.getAction();

        if (eventConf.getMode().equals(IndexingEventConf.ONLY_ASYNC)) {
            log.debug("Event with id=" + eventId
                    + " should only be processed in async");
            eventsToIgnore.add(eventId);
            return;
        }
        if (eventConf.getMode().equals(IndexingEventConf.NEVER)) {
            log.debug("Event with id=" + eventId
                    + " is desactivated for indexing");
            eventsToIgnore.add(eventId);
            return;
        }

        if (IndexingEventConf.INDEX.equals(action)
                || IndexingEventConf.RE_INDEX.equals(action)) {

            Map<String, Serializable> prefetch = DocumentModelFactory.updatePrefetch(dm);

            // Check if the dm is indexable.
            // For now only based on explicit registration of the doc type
            // against the search service. (i.e : versus core type facet based)
            if (service.getIndexableDocTypeFor(dm.getType()) == null) {
                return;
            }

            String docTitle = (String) dm.getProperty("dublincore", "title");
            if (docTitle == null || docTitle.length() == 0) {
                // Create event generated by a session.createDocument
                // => the document is created but not yet populated
                // ==> do not index it
                log.debug("empty create call");
                return;
            }

            if (prefetch == null || prefetch.keySet().isEmpty()) {
                log.debug("Prefetch is null. Regular indexing scheduled for doc@"
                        + dm.getPathAsString());
                service.index(computeResourcesFor(dm), false);
            } else {
                log.debug("Scheduling prefetch only indexing for doc@"
                        + dm.getPathAsString());
                service.index(computePrefetchedResourcesFor(dm, prefetch));
            }

        } else if (IndexingEventConf.UN_INDEX.equals(action)) {
            if (log.isDebugEnabled()) {
                log.debug("synchronous unindexing " + dm.getPath());
            }
            // Here, we only unindex synchronously @ depth = 0. The sub-children
            // will be unindex asynchronously.
            service.deleteAggregatedResources(dm.getId());
        }
    }

    public static IndexableResources computeResourcesFor(DocumentModel dm)
            throws IndexingException {
        return IndexableResourcesFactory.computeResourcesFor(dm,
                dm.getSessionId());
    }

    public static ResolvedResources computePrefetchedResourcesFor(
            DocumentModel dm, Map<String, Serializable> filters)
            throws IndexingException {

        searchService = getSearchService(); // ensure resolution
        Map<String, ResolvedResource> resources = new HashMap<String, ResolvedResource>();
        List<ResolvedData> data = new ArrayList<ResolvedData>();
        for (String k : filters.keySet()) {

            // Here the document model only supports schemaName + "." +
            // fieldName notation for prefix. If in the future we do support
            // XPath or prefixed notations then this code wll have to be
            // updated.
            String prefix = null;
            if (k.split("\\.").length == 2) {
                prefix = k.split("\\.")[0];
            } else {
                continue;
            }
            Schema schema = getTypeManager().getSchemaFromPrefix(prefix);
            if (schema == null) {
                schema = getTypeManager().getSchema(prefix);
            }
            if (schema == null) {
                continue;
            }

            // Perform and generate partial schema resolved resource generation
            // in a programatic way from the search service fields configuration
            // The prefetch data are the
            // minimum data needed for the search based virtual navigation tree
            // can be displayed.
            if (!resources.containsKey(prefix)) {
                resources.put(prefix, new ResolvedResourceImpl(dm.getId()));
                IndexableResourceConf conf = new IndexableResourceDescriptor(
                        schema.getName(), schema.getNamespace().prefix, true,
                        null, null, ResourceType.SCHEMA);
                IndexableResource proxy = new DocumentIndexableResourceImpl(dm,
                        conf, dm.getSessionId());
                resources.get(prefix).setIndexableResource(proxy);
            }
            IndexableResourceConf sConf = searchService.getIndexableResourceConfByName(
                    schema.getName(), true);
            if (sConf == null) {
                continue;
            }
            IndexableResourceDataConf dataConf = sConf.getIndexableFields().get(
                    k.split("\\.")[1]);
            if (dataConf == null) {
                continue;
            }
            resources.get(prefix).addIndexableData(
                    new ResolvedDataImpl(
                            dataConf.getIndexingName(),
                            dataConf.getIndexingAnalyzer(),
                            dataConf.getIndexingType(),
                            filters.get(k), // value
                            dataConf.isStored(), dataConf.isIndexed(),
                            dataConf.isMultiple(), dataConf.isSortable(),
                            dataConf.getSortOption(), dataConf.getTermVector(),
                            dataConf.isBinary(), null));
        }
        List<ResolvedResource> rresources = new ArrayList<ResolvedResource>();
        for (ResolvedResource resource : resources.values()) {
            rresources.add(resource);
        }

        // Compute builtins
        IndexableResourceConf bConf = getSearchService().getIndexableResourceConfByName(
                BuiltinDocumentFields.DOC_BUILTINS_RESOURCE_NAME, false);
        ResourceTypeDescriptor bDesc = getSearchService().getResourceTypeDescriptorByName(
                ResourceType.DOC_BUILTINS);
        IndexableResourceFactory factory = bDesc.getFactory();

        if (factory == null) {
            throw new IndexingException(
                    "Cannot find Nuxeo Core document builtins indexable resource factory ");
        }

        // XXX should be included with the others resoures. We need to fix
        // hardcoded stuffs backend side first relying on prefixed builtin prop
        // names.
        ResolvedResource rr = factory.resolveResourceFor(dm, bConf,
                dm.getSessionId());
        if (rr == null) {
            throw new IndexingException("Builtins are empty... Cancelling...");
        }
        data.addAll(rr.getIndexableData());

        // Construct the resulting resolved resource implementation.
        return new ResolvedResourcesImpl(dm.getId(), rresources, data,
                dm.getACP());
    }

}

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
 * $Id$
 */

package org.nuxeo.ecm.platform.relations.search.indexer;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.impl.IndexableResourcesImpl;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocType;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.search.delegate.RelationSearchBusinessDelegate;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.RelationIndexableResourceFactory;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.api.ResourceType;

/**
 * This indexer takes an object as input and indexes all relations of which the
 * object is the subject.
 *
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class RelationIndexer {

    private static final Log log = LogFactory.getLog(RelationIndexer.class);

    private static final IndexableResourceFactory factory = new RelationIndexableResourceFactory();

    /**
     *
     * Currently handles incoming DocumentModel instances only
     *
     * @param object
     * @return the set of indexable resource configurations
     * @throws IndexingException
     */
    public Set<IndexableResourceConf> getResourceConfs(Serializable object)
            throws IndexingException {
        if (!(object instanceof DocumentModel)) {
            throw new IndexingException("Can handle only DocumentModel for now");
        }
        DocumentModel dm = (DocumentModel) object;

        SearchService service = RelationSearchBusinessDelegate.getSearchService();
        String docType = dm.getType();
        IndexableDocType docTypeConf = service.getIndexableDocTypeFor(docType);
        if (docTypeConf == null) {
            log.debug("Found no indexable doc type configuration for docType="
                    + docType);
            return null;
        }

        Set<IndexableResourceConf> res = new HashSet<IndexableResourceConf>();

        for (String candidate : docTypeConf.getResources()) {
            IndexableResourceConf conf = service.getIndexableResourceConfByName(
                    candidate, false);
            if (conf == null) {
                continue;
            }
            if (conf.getType().equals(ResourceType.RELATIONS)) {
                res.add(conf);
            }
        }
        return res;
    }

    public List<IndexableResources> extractResources(Serializable object)
            throws IndexingException {
        RelationManager relationManager = RelationSearchBusinessDelegate.getRelationManager();

        String coreSessionId = null;
        if (object instanceof DocumentModel) {
            coreSessionId = ((DocumentModel) object).getSessionId();
        }

        // Each possible relations for all possible
        // (rdf) Resource representation of incoming object
        List<IndexableResources> allIndexableResources = new LinkedList<IndexableResources>();
        Set<Resource> allResources;
        try {
            allResources = relationManager.getAllResources(object);
        } catch (ClientException e) {
            throw new IndexingException(e);
        }
        if (allResources != null) {
            for (Resource r : allResources) {
                for (IndexableResourceConf conf : getResourceConfs(object)) {
                    try {
                        for (Statement statement : getStatements(
                                conf.getName(), r)) {
                            List<IndexableResource> iResources = new LinkedList<IndexableResource>();
                            // iResources.add(getOpenAcp()); TODO
                            iResources.add(factory.createIndexableResourceFrom(
                                    statement, conf, coreSessionId));
                            allIndexableResources.add(new IndexableResourcesImpl(
                                    computeResourcesGlobalKey(statement,
                                            object, iResources), iResources));
                        }
                    } catch (ClientException e) {
                        throw new IndexingException(
                                "Failed to retrieve statements" + " in graph "
                                        + conf.getName() + " for resource "
                                        + r.getUri(), e);
                    }
                }
            }
        }
        return allIndexableResources;
    }

    private static Set<Statement> getStatements(String graphName, Resource r)
            throws ClientException {
        RelationManager relationManager = RelationSearchBusinessDelegate.getRelationManager();

        Set<Statement> res = new HashSet<Statement>();
        res.addAll(relationManager.getStatements(graphName, new StatementImpl(
                r, null, null)));
        res.addAll(relationManager.getStatements(graphName, new StatementImpl(
                null, null, r)));
        return res;
    }

    public void index(Serializable object) throws IndexingException {
        SearchService searchService = RelationSearchBusinessDelegate.getSearchService();

        for (IndexableResources aggregated : extractResources(object)) {
            searchService.index(aggregated, false); // No full text
        }
    }

    private static String computeResourcesGlobalKey(Statement statement,
            Serializable object, List<IndexableResource> resources) {
        // TODO good enough ?
        return String.format("%d", statement.hashCode());
    }

    public void unIndexStatements(Collection<Statement> statements)
            throws IndexingException {
        // TODO we have potential collision risks with other aliases
        // we should lift ambiguities
        SearchService service = RelationSearchBusinessDelegate.getSearchService();
        for (Statement statement : statements) {
            service.deleteAggregatedResources(computeResourcesGlobalKey(
                    statement, null, null));
        }
    }

    public void indexStatements(String graphName,
            Collection<Statement> statements, String coreSessionId)
            throws IndexingException {

        SearchService service = RelationSearchBusinessDelegate.getSearchService();
        IndexableResourceConf iConf = service.getIndexableResourceConfByName(
                graphName, false);

        for (Statement statement : statements) {
            LinkedList<IndexableResource> iResources = new LinkedList<IndexableResource>();
            // iResources.add(getOpenAcp()); TODO
            iResources.add(factory.createIndexableResourceFrom(statement,
                    iConf, coreSessionId));
            service.index(new IndexableResourcesImpl(computeResourcesGlobalKey(
                    statement, null, iResources), iResources), false);
        }
    }

}

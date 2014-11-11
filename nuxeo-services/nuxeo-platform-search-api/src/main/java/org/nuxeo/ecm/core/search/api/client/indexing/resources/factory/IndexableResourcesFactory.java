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
 * $Id: DocumentCentricResourcesFactory.java 13416 2007-03-08 11:28:04Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentBuiltinsIndexableResourceImpl;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentIndexableResourceImpl;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.impl.IndexableResourcesImpl;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocType;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;

/**
 * Computes an <code>IndexableResources</code> instance for a given
 * DocumentModel.
 * <p>
 * This factory is specific to a Nuxeo core <code>DocumentModel</code>. Let's
 * see if we need another kind of non document-centric factory in the future.
 * This use case may appear if <code>org.nuxeo.ecm.search</code> aims at being
 * an enterprise search standalone engine in the future. Right now, this is not
 * aimed at being the case.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class IndexableResourcesFactory {

    private static final Log log = LogFactory.getLog(IndexableResourcesFactory.class);

    private static final Map<String, IndexableResourceConf> resourceConfCache
            = new ConcurrentHashMap<String, IndexableResourceConf>();
    private static final Map<String, IndexableResourceConf> fullResourceConfCache
            = new ConcurrentHashMap<String, IndexableResourceConf>();
    private static final Map<String, IndexableDocType> indexableDocTypeCache
            = new ConcurrentHashMap<String, IndexableDocType>();

    private static final IndexableDocType NULL = new IndexableDocTypeDescriptor();

    // Utility class.
    private IndexableResourcesFactory() {
    }

    private static String computeResourcesGlobalKey(DocumentModel dm) {
        // :FIXME: temp solution.
        return dm.getId();
    }

    // TODO probably unused, as sid is always null
    public static IndexableResources computeResourcesFor(DocumentModel dm) {
        if (dm == null) {
            log.error("No document model given.... Nothing to compute.");
            return null;
        }

        // Ask resource configurations for this given doctype.
        String docType = dm.getType();
        IndexableDocType docTypeConf = getIndexableDocType(docType);

        // Compute base resources configuration (including automatic schema
        // setup)
        List<String> resourceNames = null;
        List<String> autoSchemas = new ArrayList<String>();
        List<String> excludedSchemas = new ArrayList<String>();
        if (docTypeConf != null) {
            log.debug("Found a indexable doc type configuration for docType="
                    + docType);
            resourceNames = docTypeConf.getResources();
            excludedSchemas = docTypeConf.getExcludedSchemas();
            // Check if we want all schema indexable.
            // In this case merge them with the one specified.
            if (docTypeConf.areAllSchemasIndexable()) {
                log.debug("All schemas will be scheduled to be indexed by configuration");
                String[] declaredSchemas = dm.getSchemas();
                if (declaredSchemas == null) {
                    declaredSchemas = new String[0];
                }
                autoSchemas = Arrays.asList(dm.getSchemas());
            }
        }

        // Computes corresponding indexable resources.
        List<IndexableResource> resources = new ArrayList<IndexableResource>();
        String sid = null;
        if (resourceNames != null) {
            for (String resourceName : resourceNames) {
                IndexableResourceConf conf = getResourceConf(resourceName, false);
                if (conf != null) {

                    log.debug("Found indexable resource configuration with name: "
                            + resourceName);

                    // XXX do not rely on this here => should be handled
                    // resource instance side for generalization purpose.
                    if (conf.getType().equals(ResourceType.SCHEMA)) {

                        // Check if this schema is explicitly excluded. This
                        // case might happen if the configuration is badly
                        // done.
                        String schemaName = conf.getName();
                        if (excludedSchemas.contains(schemaName)) {
                            log.debug("Exclude schema by configuration. schema: "
                                    + schemaName);
                            continue;
                        }

                        conf = getResourceConf(schemaName, true);

                        // Check if the schema is an actual declared schema for
                        // this document model.
                        String[] schemas = dm.getSchemas();
                        Collection<String> lschemas = Arrays.asList(schemas);
                        if (lschemas.contains(schemaName)) {
                            resources.add(new DocumentIndexableResourceImpl(dm,
                                    conf, sid));
                        }
                    }
                    // :XXX: handle other cases.
                } else {
                    log.error("No registered indexing configuration for resource: "
                            + resourceName);
                }
            }
        } else {
            log.debug(String.format(
                    "No indexable resources found for docType '%s'. Checking schema base configuration now.",
                    docType));
        }

        // Deal with automatic schema fetch.
        for (String schemaName : autoSchemas) {

            // If already full defined as a resource
            if (resourceNames.contains(schemaName)) {
                log.debug("Discarding automatic indexing for.... schemaName="
                        + schemaName);
                log.debug("Because already defined as a full resource conf... check conf...");
                continue;
            }

            // case might happen if the configuration is badly
            // done.
            if (excludedSchemas.contains(schemaName)) {
                log.debug("Exclude schema by configuration. schema="
                        + schemaName);
                continue;
            }

            IndexableResourceConf conf = getResourceConf(schemaName, true);
            resources.add(new DocumentIndexableResourceImpl(dm, conf, sid));

        }

        // Automatically add builtins
        IndexableResourceConf builtinConf = getResourceConf(
                BuiltinDocumentFields.DOC_BUILTINS_RESOURCE_NAME, false);

        if (builtinConf != null) {
            resources.add(new DocumentBuiltinsIndexableResourceImpl(dm,
                    builtinConf, sid));
        }

        // Compute the global identifier.
        return new IndexableResourcesImpl(computeResourcesGlobalKey(dm),
                resources);
    }

    // Caching methods
    private static IndexableDocType getIndexableDocType(String type) {
        IndexableDocType res = indexableDocTypeCache.get(type);

        if (res == null) {
            SearchService service = SearchServiceDelegate.getRemoteSearchService();
            res = service.getIndexableDocTypeFor(type);
            if (res == null) {
                res = NULL;
            }
            synchronized (indexableDocTypeCache) {
                indexableDocTypeCache.put(type, res);
            }
        }
        if (res == NULL) {
            return null;
        }
        return res;
    }

    private static IndexableResourceConf getResourceConf(String resourceName, Boolean full) {
        IndexableResourceConf res;

        if (full) {
            res = fullResourceConfCache.get(resourceName);
        } else {
            res = resourceConfCache.get(resourceName);
        }

        if (res == null) {
            SearchService service = SearchServiceDelegate.getRemoteSearchService();
            res = service.getIndexableResourceConfByName(resourceName, full);
            setIndexableResourceConfIntoCache(resourceName, full, res);
        }

        return res;
    }

    private static synchronized void setIndexableResourceConfIntoCache(String resourceName,
            Boolean full, IndexableResourceConf conf) {
        if (full) {
            fullResourceConfCache.put(resourceName, conf);
        } else {
            resourceConfCache.put(resourceName, conf);
        }
    }

}

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
 * $Id: ResolvedResourcesFactory.java 27190 2007-11-14 17:03:26Z gracinet $
 */

package org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.LazyBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedDataImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourceImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourcesImpl;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.indexing.blobs.BlobExtractor;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.DocumentIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;

/**
 * Computes a <code>ResolvedResources</code> instance.
 * <p>
 * Here, the idea is to aggregate the configuration and the actual indexable
 * data value from a list of prepared resources expected to be already
 * associated with their configurations beforehand.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class ResolvedResourcesFactory {

    private static final Log log = LogFactory.getLog(ResolvedResourcesFactory.class);

    private static SearchService service;

    // Utiliy class.
    private ResolvedResourcesFactory() {
    }

    /**
     * Reinitializes the search service. To be used by unit tests only
     */
    public static void reInitSearchService() {
        service = null;
    }

    private static SearchService getSearchService() {
        if (service == null) {
            service = SearchServiceDelegate.getRemoteSearchService();
        }
        return service;
    }

    /**
     * Computes the fulltext for a given indexable resource.
     * <p>
     * This should be moved somewhere else where configurable.
     *
     * @param resources a set of document indexable resources
     * @return a resolved data instance
     */
    private static ResolvedData computeFulltext(IndexableResources resources) {

        String value = "";
        String analyzer = "default";
        String type = "text";

        SearchService service = getSearchService();
        if (service != null) {
            String prefixedName = BuiltinDocumentFields.getPrefixedNameFor(BuiltinDocumentFields.FIELD_FULLTEXT);
            FulltextFieldDescriptor desc = getSearchService().getFullTextDescriptorByName(
                    prefixedName);
            if (desc != null) {

                analyzer = desc.getAnalyzer();

                List<String> resourceFields = desc.getResourceFields();
                List<String> extracted = new ArrayList<String>(
                        resourceFields.size());
                for (String field : resourceFields) {
                    extracted.add(extractForFullText(resources, desc, field));
                }
                value = StringUtils.join(extracted, " ");
            } else {
                log.warn("No fulltext descriptor found for name="
                        + BuiltinDocumentFields.FIELD_FULLTEXT);
            }
        }

        return new ResolvedDataImpl(BuiltinDocumentFields.FIELD_FULLTEXT,
                analyzer, type, value, false, true, false, false, null, null,
                false, null);
    }

    /**
     * At this point, resources cannot be empty any more.
     *
     * @param resources
     * @param desc
     * @param field
     * @return
     */
    private static String extractForFullText(IndexableResources resources,
            FulltextFieldDescriptor desc, String field) {
        String[] sfield = field.split(":", 2);
        if (sfield.length < 2) {
            log.warn("Discarding field conf for fulltext. Invalid syntax :"
                    + field);
            return "";
        }
        String resourceName = sfield[0].trim();
        String dataKeyName = sfield[1].trim();
        if ("*".equals(resourceName) && "*".equals(dataKeyName)) {
            return extractAllForFullText(desc, resources);
        }
        if ("*".equals(dataKeyName)) {
            return extractAllForFullText(resourceName, desc, resources);
        }
        Serializable ob = null;
        IndexableResource resource = resources.getIndexableResources().get(0);
        try {
            ob = resource.getValueFor(resourceName + ':' + dataKeyName);
        } catch (IndexingException ie) {
            log.warn("Got an exception while retrieving " + field + "on "
                    + resource.toString());
            return "";
        }
        if (ob == null) {
            return "";
        }
        return convertForFullText(ob, desc, resources);
    }

    public static String extractAllForFullText(String resourceName,
            FulltextFieldDescriptor desc, IndexableResources resources) {
        for (IndexableResource resource : resources.getIndexableResources()) {
            if (resourceName.equals(resource.getConfiguration().getName())) {
                return extractAllForFullText(desc, resource, resources);
            }
        }
        log.debug("Resource '" + resourceName + "' not found on " + resources);
        return "";
    }

    public static String extractAllForFullText(FulltextFieldDescriptor desc,
            IndexableResource resource, IndexableResources resources) {
        List<String> res = new LinkedList<String>();
        Map<String, IndexableResourceDataConf> fieldMap = resource.getConfiguration().getIndexableFields();
        for (String fieldName : fieldMap.keySet()) {
            String type = fieldMap.get(fieldName).getIndexingType();
            if (type == null) {
                continue;
            }
            type = type.toLowerCase();
            if (!"text".equals(type) && !"keyword".equals(type)) {
                continue;
            }

            Serializable v;
            try {
                v = resource.getValueFor(resource.getName() + ":" + fieldName);
            } catch (IndexingException e) {
                log.warn(String.format("could not get value of %s on %s",
                        fieldName, resource.toString()), e);
                continue;
            }
            if (v == null) {
                continue;
            }
            res.add(convertForFullText(v, desc, resources));
        }
        // TODO PERF GR: avoid repeated calls to join (mem and cpu consuming)
        return StringUtils.join(res, " ");
    }

    public static String extractAllForFullText(FulltextFieldDescriptor desc,
            IndexableResources resources) {
        List<String> res = new LinkedList<String>();

        for (IndexableResource resource : resources.getIndexableResources()) {
            if (resource.getConfiguration().getType().equals(
                    ResourceType.DOC_BUILTINS)) {
                continue;
            }
            res.add(extractAllForFullText(desc, resource, resources));
        }
        return StringUtils.join(res, " ");
    }

    @SuppressWarnings("unchecked")
    private static String convertForFullText(Serializable ob,
            FulltextFieldDescriptor desc, IndexableResources resources) {
        if (ob instanceof String) {
            return (String) ob;
        } else if (ob instanceof Blob) {
            Blob blob = (Blob) ob;
            return blobToText(blob, desc, resources);
        } else if (ob instanceof List) {
            List<Serializable> l = (List<Serializable>) ob;
            List<String> res = new ArrayList<String>(l.size());
            for (Serializable i : l) {
                res.add(convertForFullText(i, desc, resources));
            }
            return StringUtils.join(res, " ");
        } else if (ob instanceof String[]) {
            return StringUtils.join((String[]) ob, " ");
        }
        return "";
    }

    private static String blobToText(Blob blob, FulltextFieldDescriptor desc,
            IndexableResources resources) {

        String mimeType = blob.getMimeType();
        if (mimeType.equals("text/plain")) {
            try {
                return blob.getString();
            } catch (IOException e) {
                log.warn("Couldn't convert to fulltext..." + e.getMessage());
                return "";
            }
        }

        String res = "";
        BlobExtractor extractor = service.getBlobExtractorByName(desc.getBlobExtractorName());
        if (extractor != null) {
            try {
                if (!(blob instanceof StreamingBlob)
                        && !(blob instanceof LazyBlob)) {
                    log.warn("Blob to be extracted is NOT a StreamingBlob instance... "
                            + "Might be inefficient in case of large binaries...");
                }
                res = extractor.extract(blob, mimeType, desc);
            } catch (Throwable t) {
                // We don't want the whole indexing process to fail if there is
                // an underlying error transformation plugin side.
                log.error("Cannot extract blob content using transformer..."
                        + " Canceling. Fulltext won't be available completely "
                        + " for this document", t);
            }
        } else {
            log.warn("No blob extractor found within the full text descriptor field. Please check out your configuration.");
        }

        blob = null;
        return res;
    }

    public static ResolvedResources computeAggregatedResolvedResourcesFrom(
            IndexableResources resources, boolean fulltext)
            throws IndexingException {

        List<ResolvedData> commonData = new ArrayList<ResolvedData>();
        if (fulltext && !resources.getIndexableResources().isEmpty()) {
            commonData.add(computeFulltext(resources));
        }

        ACP acp = null;
        List<ResolvedResource> resolvedResources = new ArrayList<ResolvedResource>();

        boolean acpComputed = false;
        boolean builtinComputed = false;
        for (IndexableResource resource : resources.getIndexableResources()) {

            IndexableResourceConf conf = resource.getConfiguration();

            // XXX should not rely on this => move this code within the factory.
            if (conf.getType().equals(ResourceType.SCHEMA)) {

                List<ResolvedData> resolvedDatas = new ArrayList<ResolvedData>();
                DocumentIndexableResource docResource = (DocumentIndexableResource) resource;

                try {

                    for (IndexableResourceDataConf dataConf : conf.getIndexableFields().values()) {

                        String schemaName = conf.getName();
                        String name = dataConf.getIndexingName();

                        Object value = null;

                        String computedKey = schemaName + ':' + name;
                        value = resource.getValueFor(computedKey);
                        resolvedDatas.add(new ResolvedDataImpl(
                                dataConf.getIndexingName(),
                                dataConf.getIndexingAnalyzer(),
                                dataConf.getIndexingType(), value,
                                dataConf.isStored(), dataConf.isIndexed(),
                                dataConf.isMultiple(), dataConf.isSortable(),
                                dataConf.getSortOption(),
                                dataConf.getTermVector(), dataConf.isBinary(),
                                dataConf.getProperties()));
                    }

                    if (!acpComputed) {
                        acp = docResource.getDocMergedACP();
                        acpComputed = true;
                    }

                    resolvedResources.add(new ResolvedResourceImpl(
                            resources.getId(), resource, resolvedDatas));
                } finally {
                    // Disconnect inner core session
                    //docResource.closeCoreSession();
                }
            } else {
                // Generic code using factory

                // Fetch the corresponding resource type descriptor.
                String resourceType = conf.getType();
                ResourceTypeDescriptor resourceTypeDesc = getSearchService().getResourceTypeDescriptorByName(
                        resourceType);

                if (resourceTypeDesc == null) {
                    throw new IndexingException(
                            "Cannot find associated resource type descriptor for resource type = "
                                    + resourceType);
                }

                // Fetch the factory from the resource descriptor.
                IndexableResourceFactory factory = resourceTypeDesc.getFactory();
                if (factory == null) {
                    throw new IndexingException(
                            "Cannot find associated factoryfor resource type = "
                                    + resourceType);
                }

                // XXX
                ResolvedResource rr = factory.resolveResourceFor(resource);
                if (!acpComputed) {
                    // GR needed for non schema resources (typically singletons)
                    acp = resource.computeAcp();
                }
                if (resourceType.equals(ResourceType.DOC_BUILTINS)) {
                    if (!builtinComputed) {
                        commonData.addAll(rr.getIndexableData());
                        builtinComputed = true;
                    }
                } else {
                    if (rr != null) {
                        resolvedResources.add(rr);
                    } else {
                        log.debug("Resolved resource " + resourceType
                                + " is empty. Ignoring");
                    }
                }
            }

        }

        // Resolve.
        ResolvedResources results = new ResolvedResourcesImpl(
                resources.getId(), resolvedResources, commonData, acp);

        // :TODO:
        return results;
    }
}

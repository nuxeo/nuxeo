/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.api.blobholder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapter;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapterDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component to manage the pluggable factory for {@link DocumentAdapterFactory}.
 * <p>
 * Also provides the service interface {@link BlobHolderAdapterService}
 *
 * @author tiry
 */
public class BlobHolderAdapterComponent extends DefaultComponent implements BlobHolderAdapterService {

    private static final Log log = LogFactory.getLog(BlobHolderAdapterComponent.class);

    public static final String BLOBHOLDERFACTORY_EP = "BlobHolderFactory";

    public static final String EXTERNALBLOB_ADAPTER_EP = "ExternalBlobAdapter";

    protected final Map<String, BlobHolderFactory> factories = new HashMap<>();

    protected Map<String, BlobHolderFactory> factoriesByFacets = new HashMap<>();

    protected static final Map<String, ExternalBlobAdapter> externalBlobAdapters = new HashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (BLOBHOLDERFACTORY_EP.equals(extensionPoint)) {
            BlobHolderFactoryDescriptor desc = (BlobHolderFactoryDescriptor) contribution;
            String docType = desc.getDocType();
            if (docType != null) {
                factories.put(docType, desc.getFactory());
            }
            String facet = desc.getFacet();
            if (facet != null) {
                factoriesByFacets.put(facet, desc.getFactory());
            }
        } else if (EXTERNALBLOB_ADAPTER_EP.equals(extensionPoint)) {
            ExternalBlobAdapterDescriptor desc = (ExternalBlobAdapterDescriptor) contribution;
            ExternalBlobAdapter adapter = desc.getAdapter();
            String prefix = desc.getPrefix();
            if (externalBlobAdapters.containsKey(prefix)) {
                log.info(String.format("Overriding external blob adapter with prefix '%s'", prefix));
                externalBlobAdapters.remove(prefix);
            }
            adapter.setPrefix(desc.getPrefix());
            adapter.setProperties(desc.getProperties());
            externalBlobAdapters.put(desc.getPrefix(), adapter);
            log.info(String.format("Registered external blob adapter with prefix '%s'", prefix));
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
    }

    /* for test */

    public static Set<String> getFactoryNames() {
        return ((BlobHolderAdapterComponent) Framework.getLocalService(
                BlobHolderAdapterService.class)).factories.keySet();
    }

    /* Service Interface */

    @Override
    public ExternalBlobAdapter getExternalBlobAdapterForPrefix(String prefix) {
        return externalBlobAdapters.get(prefix);
    }

    @Override
    public ExternalBlobAdapter getExternalBlobAdapterForUri(String uri) {
        if (uri != null && uri.contains(ExternalBlobAdapter.PREFIX_SEPARATOR)) {
            String prefix = uri.substring(0, uri.indexOf(ExternalBlobAdapter.PREFIX_SEPARATOR));
            return getExternalBlobAdapterForPrefix(prefix);
        }
        return null;
    }

    @Override
    public Blob getExternalBlobForUri(String uri) throws PropertyException, IOException {
        ExternalBlobAdapter adapter = getExternalBlobAdapterForUri(uri);
        if (adapter == null) {
            throw new PropertyException(String.format("No external blob adapter found for uri '%s'", uri));
        }
        return adapter.getBlob(uri);
    }

    @Override
    public BlobHolder getBlobHolderAdapter(DocumentModel doc) {
        if (factories.containsKey(doc.getType())) {
            BlobHolderFactory factory = factories.get(doc.getType());
            return factory.getBlobHolder(doc);
        }

        for (Map.Entry<String, BlobHolderFactory> entry : factoriesByFacets.entrySet()) {
            if (doc.hasFacet(entry.getKey())) {
                return entry.getValue().getBlobHolder(doc);
            }
        }

        if (doc.hasSchema("file")) {
            return new DocumentBlobHolder(doc, "file:content");
        } else if (doc.hasSchema("note")) {
            String mt = null;
            try {
                mt = (String) doc.getPropertyValue("note:mime_type");
                if (mt == null) {
                    String note = (String) doc.getPropertyValue("note:note");
                    if (note != null && !"".equals(note)) {
                        mt = "text/plain"; // BBB
                    }
                }
            } catch (PropertyException e) {
                // mt = null;
            }
            return new DocumentStringBlobHolder(doc, "note:note", mt);
        }
        return null;
    }

}

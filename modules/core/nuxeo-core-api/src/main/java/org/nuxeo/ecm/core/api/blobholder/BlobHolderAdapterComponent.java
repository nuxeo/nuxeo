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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapter;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapterDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component to manage the pluggable factory for {@link DocumentAdapterFactory}.
 * <p>
 * Also provides the service interface {@link BlobHolderAdapterService}
 *
 * @author tiry
 */
public class BlobHolderAdapterComponent extends DefaultComponent implements BlobHolderAdapterService {

    public static final String BLOBHOLDERFACTORY_EP = "BlobHolderFactory";

    public static final String EXTERNALBLOB_ADAPTER_EP = "ExternalBlobAdapter";

    protected Map<String, BlobHolderFactory> factoriesByName;

    protected Map<String, BlobHolderFactory> factoriesByType;

    protected Map<String, BlobHolderFactory> factoriesByFacets;

    protected Map<String, ExternalBlobAdapter> externalBlobAdapters;

    @Override
    public void start(ComponentContext context) {
        factoriesByName = new HashMap<>();
        factoriesByType = new HashMap<>();
        factoriesByFacets = new HashMap<>();
        this.<BlobHolderFactoryDescriptor> getRegistryContributions(BLOBHOLDERFACTORY_EP).forEach(desc -> {
            String name = desc.getName();
            if (StringUtils.isNotBlank(name)) {
                factoriesByName.put(name, desc.getFactory());
            }
            String docType = desc.getDocType();
            if (docType != null) {
                factoriesByType.put(docType, desc.getFactory());
            }
            String facet = desc.getFacet();
            if (facet != null) {
                factoriesByFacets.put(facet, desc.getFactory());
            }
        });
        externalBlobAdapters = new HashMap<>();
        this.<ExternalBlobAdapterDescriptor> getRegistryContributions(EXTERNALBLOB_ADAPTER_EP).forEach(desc -> {
            ExternalBlobAdapter adapter = desc.getAdapter();
            adapter.setPrefix(desc.getPrefix());
            adapter.setProperties(desc.getProperties());
            externalBlobAdapters.put(desc.getPrefix(), adapter);
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        factoriesByName = null;
        factoriesByType = null;
        factoriesByFacets = null;
        externalBlobAdapters = null;
    }

    /* for test */

    public Set<String> getFactoryNames() {
        return factoriesByType.keySet();
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
    public BlobHolder getBlobHolderAdapter(DocumentModel doc, String factoryName) {
        if (factoryName != null && factoriesByName.containsKey(factoryName)) {
            BlobHolderFactory factory = factoriesByName.get(factoryName);
            return factory.getBlobHolder(doc);
        }

        if (factoriesByType.containsKey(doc.getType())) {
            BlobHolderFactory factory = factoriesByType.get(doc.getType());
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

    @Override
    public BlobHolder getBlobHolderAdapter(DocumentModel doc) {
        return getBlobHolderAdapter(doc, null);
    }

}

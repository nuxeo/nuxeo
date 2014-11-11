/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.api.blobholder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapter;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapterDescriptor;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component to manage the pluggable factory for
 * {@link DocumentAdapterFactory}.
 * <p>
 * Also provides the service interface {@link BlobHolderAdapterService}
 *
 * @author tiry
 */
public class BlobHolderAdapterComponent extends DefaultComponent implements
        BlobHolderAdapterService {

    private static final Log log = LogFactory.getLog(BlobHolderAdapterComponent.class);

    public static final String BLOBHOLDERFACTORY_EP = "BlobHolderFactory";

    public static final String EXTERNALBLOB_ADAPTER_EP = "ExternalBlobAdapter";

    protected final Map<String, BlobHolderFactory> factories
            = new HashMap<String, BlobHolderFactory>();

    protected Map<String, BlobHolderFactory> factoriesByFacets
                = new HashMap<String, BlobHolderFactory>();

    protected static final Map<String, ExternalBlobAdapter> externalBlobAdapters
            = new HashMap<String, ExternalBlobAdapter>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

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
                log.info(String.format(
                        "Overriding external blob adapter with prefix '%s'",
                        prefix));
                externalBlobAdapters.remove(prefix);
            }
            adapter.setPrefix(desc.getPrefix());
            adapter.setProperties(desc.getProperties());
            externalBlobAdapters.put(desc.getPrefix(), adapter);
            log.info(String.format(
                    "Registered external blob adapter with prefix '%s'", prefix));
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    /* for test */

    public static Set<String> getFactoryNames() {
        return ((BlobHolderAdapterComponent)Framework.getLocalService(BlobHolderAdapterService.class)).factories.keySet();
    }

    /* Service Interface */

    @Override
    public ExternalBlobAdapter getExternalBlobAdapterForPrefix(String prefix) {
        return externalBlobAdapters.get(prefix);
    }

    @Override
    public ExternalBlobAdapter getExternalBlobAdapterForUri(String uri) {
        if (uri != null && uri.contains(ExternalBlobAdapter.PREFIX_SEPARATOR)) {
            String prefix = uri.substring(0,
                    uri.indexOf(ExternalBlobAdapter.PREFIX_SEPARATOR));
            return getExternalBlobAdapterForPrefix(prefix);
        }
        return null;
    }

    @Override
    public Blob getExternalBlobForUri(String uri) throws PropertyException {
        ExternalBlobAdapter adapter = getExternalBlobAdapterForUri(uri);
        if (adapter == null) {
            throw new PropertyException(String.format(
                    "No external blob adapter found for uri '%s'", uri));
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
            return new DocumentBlobHolder(doc, "file:content", "file:filename");
        } else if (doc.hasSchema("note")) {
            try {
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
            } catch (Exception e) {
                log.error(e);
            }
        }
        return null;
    }

}

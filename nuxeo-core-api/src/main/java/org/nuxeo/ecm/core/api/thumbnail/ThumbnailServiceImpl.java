/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.core.api.thumbnail;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterComponent;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Thumbnail service providing 3 kind of factories: by facet, by doctype, and
 * thumbnail default one
 * 
 * @since 5.7
 */
public class ThumbnailServiceImpl extends DefaultComponent implements
        ThumbnailService {

    private static final Log log = LogFactory.getLog(BlobHolderAdapterComponent.class);

    public static final String THUMBNAILFACTORY_EP = "thumbnailFactory";

    protected static ThumbnailFactory defaultFactory;

    protected static final Map<String, ThumbnailFactory> factoriesByDocType = new HashMap<String, ThumbnailFactory>();

    protected static final Map<String, ThumbnailFactory> factoriesByFacets = new HashMap<String, ThumbnailFactory>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (THUMBNAILFACTORY_EP.equals(extensionPoint)) {
            ThumbnailFactoryDescriptor desc = (ThumbnailFactoryDescriptor) contribution;
            String docType = desc.getDocType();
            if (docType != null) {
                factoriesByDocType.put(docType, desc.getFactory());
            }
            String facet = desc.getFacet();
            if (facet != null) {
                factoriesByFacets.put(facet, desc.getFactory());
            }
            if (docType == null && facet == null) {
                defaultFactory = desc.getFactory();
            }
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    public Set<String> getFactoryByDocTypeNames() {
        return factoriesByDocType.keySet();
    }

    public Set<String> getFactoryByFacetNames() {
        return factoriesByFacets.keySet();
    }

    public ThumbnailFactory getDefaultFactory() {
        return defaultFactory;
    }

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session)
            throws ClientException {
        ThumbnailFactory factory = getThumbnailFactory(doc, session);
        return factory.getThumbnail(doc, session);
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session)
            throws ClientException {
        ThumbnailFactory factory = getThumbnailFactory(doc, session);
        return factory.computeThumbnail(doc, session);
    }

    public ThumbnailFactory getThumbnailFactory(DocumentModel doc,
            CoreSession session) throws ClientException {
        if (factoriesByDocType.containsKey(doc.getType())) {
            ThumbnailFactory factory = factoriesByDocType.get(doc.getType());
            return factory;
        }
        for (Map.Entry<String, ThumbnailFactory> entry : factoriesByFacets.entrySet()) {
            if (doc.hasFacet(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (defaultFactory == null) {
            throw new ClientException(
                    "Please contribute a default thumbnail factory");
        }
        return defaultFactory;
    }
}

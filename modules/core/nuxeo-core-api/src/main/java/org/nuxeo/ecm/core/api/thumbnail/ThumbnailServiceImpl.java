/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.core.api.thumbnail;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Thumbnail service providing 3 kind of factories: by facet, by doctype, and thumbnail default one
 *
 * @since 5.7
 */
public class ThumbnailServiceImpl extends DefaultComponent implements ThumbnailService {

    public static final String THUMBNAILFACTORY_EP = "thumbnailFactory";

    protected ThumbnailFactory defaultFactory;

    protected Map<String, ThumbnailFactory> factoriesByDocType;

    protected Map<String, ThumbnailFactory> factoriesByFacets;

    @Override
    public void start(ComponentContext context) {
        factoriesByDocType = new HashMap<>();
        factoriesByFacets = new HashMap<>();
        this.<ThumbnailFactoryDescriptor> getRegistryContributions(THUMBNAILFACTORY_EP).forEach(desc -> {
            ThumbnailFactory factory = desc.getFactory();
            String docType = desc.getDocType();
            if (docType != null) {
                factoriesByDocType.put(docType, factory);
            }
            String facet = desc.getFacet();
            if (facet != null) {
                factoriesByFacets.put(facet, factory);
            }
            if (docType == null && facet == null) {
                defaultFactory = factory;
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        defaultFactory = null;
        factoriesByDocType = null;
        factoriesByFacets = null;
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
    public Blob getThumbnail(DocumentModel doc, CoreSession session) {
        ThumbnailFactory factory = getThumbnailFactory(doc, session);
        return factory.getThumbnail(doc, session);
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        ThumbnailFactory factory = getThumbnailFactory(doc, session);
        return factory.computeThumbnail(doc, session);
    }

    public ThumbnailFactory getThumbnailFactory(DocumentModel doc, CoreSession session) {
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
            throw new NuxeoException("Please contribute a default thumbnail factory");
        }
        return defaultFactory;
    }
}

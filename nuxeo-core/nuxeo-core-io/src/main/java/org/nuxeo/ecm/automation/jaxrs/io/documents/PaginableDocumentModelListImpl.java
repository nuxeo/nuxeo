/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.automation.core.util.PaginablePageProvider;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Wraps a {@link PageProvider} as a {@link DocumentModelList}
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class PaginableDocumentModelListImpl extends PaginablePageProvider<DocumentModel> implements
        PaginableDocumentModelList {

    private static final long serialVersionUID = 1L;

    protected String documentLinkBuilder;

    public PaginableDocumentModelListImpl(PageProvider<DocumentModel> provider) {
        this(provider, null);
    }

    /**
     * Creates a {@code PaginableDocumentModelListImpl} with more display information.
     *
     * @param documentLinkBuilder the name of what will be used to compute the document URLs, usually a codec name.
     * @since 5.6
     */
    public PaginableDocumentModelListImpl(PageProvider<DocumentModel> provider, String documentLinkBuilder) {
        super(provider);
        this.documentLinkBuilder = documentLinkBuilder;
    }

    public PageProvider<DocumentModel> getProvider() {
        return pageProvider;
    }

    @Override
    public String getDocumentLinkBuilder() {
        return documentLinkBuilder;
    }

    @Override
    public long totalSize() {
        return pageProvider.getResultsCount();
    }
}

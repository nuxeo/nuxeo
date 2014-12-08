/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.automation.core.util.PaginablePageProvider;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Wraps a {@link PageProvider} as a {@link DocumentList}
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

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
package org.nuxeo.ecm.automation.core.operations.services;

import javax.swing.event.DocumentListener;

import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Wraps a {@link PageProvider} as a {@link DocumentListener}
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class PaginableDocumentModelListImpl extends DocumentModelListImpl
        implements PaginableDocumentModelList {

    private static final long serialVersionUID = 1L;

    protected final PageProvider<DocumentModel> provider;

    public PaginableDocumentModelListImpl(PageProvider<DocumentModel> provider) {
        super(provider.getCurrentPage());
        this.provider = provider;
        this.totalSize = provider.getResultsCount();
    }

    public PageProvider<DocumentModel> getProvider() {
        return provider;
    }

    public long getCurrentPageIndex() {
        return provider.getCurrentPageIndex();
    }

    public long getNumberOfPages() {
        return provider.getNumberOfPages();
    }

    public long getPageSize() {
        return provider.getPageSize();
    }

}

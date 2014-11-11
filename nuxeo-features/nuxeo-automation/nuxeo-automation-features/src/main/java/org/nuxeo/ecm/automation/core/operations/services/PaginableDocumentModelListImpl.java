/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
public class PaginableDocumentModelListImpl extends DocumentModelListImpl implements PaginableDocumentModelList {

    private static final long serialVersionUID = 1L;

    protected final PageProvider<DocumentModel> provider;

    public PaginableDocumentModelListImpl(PageProvider<DocumentModel> provider) {
        super(provider.getCurrentPage());
        this.provider = provider;
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

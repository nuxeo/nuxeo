/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.query.core;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;

/**
 * Wraps a {@link DocumentModelList} inside a
 * {@link org.nuxeo.ecm.platform.query.api.PageProvider}.
 * <p>
 * This page provider does not handle pagination at all, there is only one page
 * with all the documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class DocumentModelListPageProvider extends
        AbstractPageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    protected final DocumentModelList docs;

    public DocumentModelListPageProvider() {
        this.docs = new DocumentModelListImpl();
    }

    public DocumentModelListPageProvider(DocumentModelList docs) {
        this.docs = docs;
    }

    public void setDocumentModelList(List<DocumentModel> docs) {
        this.docs.addAll(docs);
    }

    public DocumentModelList getDocumentModelList() {
        return new DocumentModelListImpl(docs);
    }

    @Override
    public List<DocumentModel> getCurrentPage() {
        return docs;
    }

    @Override
    public long getResultsCount() {
        return docs.totalSize();
    }

    @Override
    public long getPageSize() {
        return docs.totalSize();
    }

    @Override
    public long getCurrentPageSize() {
        return docs.totalSize();
    }

    @Override
    public long getNumberOfPages() {
        return 1;
    }
}

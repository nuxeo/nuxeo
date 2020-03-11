/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.query.core;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;

/**
 * Wraps a {@link DocumentModelList} inside a {@link org.nuxeo.ecm.platform.query.api.PageProvider}.
 * <p>
 * This page provider does not handle pagination at all, there is only one page with all the documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class DocumentModelListPageProvider extends AbstractPageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    protected final DocumentModelList docs;

    public DocumentModelListPageProvider() {
        docs = new DocumentModelListImpl();
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

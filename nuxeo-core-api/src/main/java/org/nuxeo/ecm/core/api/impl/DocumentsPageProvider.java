/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.AbstractLegacyDocumentPageProvider;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PageProvider;

/**
 * Keeps track of current page and previous pages loaded from document
 * iterator.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class DocumentsPageProvider extends
        AbstractLegacyDocumentPageProvider<DocumentModel> implements
        PageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    /**
     * Reference to Documents iterator from which this class is feeding pages.
     */
    protected final DocumentModelIterator docsIterator;

    protected List<DocumentModelList> loadedPages;

    /**
     * Constructor taking as argument an iterator. The iterator is considered
     * unaltered
     *
     * @param docsIterator
     * @param pageSize
     */
    public DocumentsPageProvider(DocumentModelIterator docsIterator,
            int pageSize) {
        this.docsIterator = docsIterator;
        this.pageSize = pageSize;
        resultsCount = docsIterator.size();
    }

    @Override
    public List<DocumentModel> getCurrentPage() {
        if (loadedPages == null) {
            loadedPages = new ArrayList<DocumentModelList>();
            int index = 0;
            while (index < getNumberOfPages()) {
                index++;
                if (docsIterator.hasNext()) {
                    // cache the page
                    final DocumentModelList docsPage = new DocumentModelListImpl();
                    if (pageSize == 0) {
                        while (docsIterator.hasNext()) {
                            docsPage.add(docsIterator.next());
                        }
                    } else {
                        for (int i = 0; i < pageSize; i++) {
                            if (docsIterator.hasNext()) {
                                docsPage.add(docsIterator.next());
                            } else {
                                break;
                            }
                        }
                    }
                    loadedPages.add(docsPage);
                } else {
                    // requested page out of limit
                    loadedPages.add(new DocumentModelListImpl());
                }
            }
        }

        return loadedPages.get(Long.valueOf(getCurrentPageIndex()).intValue());
    }

}

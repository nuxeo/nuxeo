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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.impl;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;

public class EmptyResultsProvider implements PagedDocumentsProvider {

    private static final long serialVersionUID = 1090501391257515681L;

    @Override
    public DocumentModelList getCurrentPage() {
        return new DocumentModelListImpl();
    }

    @Override
    public int getCurrentPageIndex() {
        return 0;
    }

    @Override
    public int getCurrentPageOffset() {
        return 0;
    }

    @Override
    public int getCurrentPageSize() {
        return 0;
    }

    @Override
    public String getCurrentPageStatus() {
        return "";
    }

    @Override
    public DocumentModelList getNextPage() {
        return null;
    }

    @Override
    public int getNumberOfPages() {
        return 0;
    }

    @Override
    public DocumentModelList getPage(int page) {
        return null;
    }

    @Override
    public long getResultsCount() {
        return 0;
    }

    @Override
    public boolean isNextPageAvailable() {
        return false;
    }

    @Override
    public boolean isPreviousPageAvailable() {
        return false;
    }

    @Override
    public void last() {
    }

    @Override
    public void next() {
    }

    @Override
    public void previous() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void rewind() {
    }

    @Override
    public int getPageSize() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public SortInfo getSortInfo() {
        return null;
    }

    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    public void setName(String name) {
    }

}

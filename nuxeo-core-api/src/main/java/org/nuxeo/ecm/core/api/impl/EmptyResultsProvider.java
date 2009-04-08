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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;

public class EmptyResultsProvider implements PagedDocumentsProvider {

    private static final long serialVersionUID = 1090501391257515681L;

    public DocumentModelList getCurrentPage() {
        return new DocumentModelListImpl();
    }

    public int getCurrentPageIndex() {
        return 0;
    }

    public int getCurrentPageOffset() {
        return 0;
    }

    public int getCurrentPageSize() {
        return 0;
    }

    public String getCurrentPageStatus() {
        return "";
    }

    public DocumentModelList getNextPage() {
        return null;
    }

    public int getNumberOfPages() {
        return 0;
    }

    public DocumentModelList getPage(int page) {
        return null;
    }

    public long getResultsCount() {
        return 0;
    }

    public boolean isNextPageAvailable() {
        return false;
    }

    public boolean isPreviousPageAvailable() {
        return false;
    }

    public void last() {
    }

    public void next() {
    }

    public void previous() {
    }

    public void refresh() {
    }

    public void rewind() {
    }

    public int getPageSize() {
        return 0;
    }

    public String getName() {
        return null;
    }

    public SortInfo getSortInfo() {
        return null;
    }

    public boolean isSortable() {
        return false;
    }

    public void setName(String name) {
    }

}

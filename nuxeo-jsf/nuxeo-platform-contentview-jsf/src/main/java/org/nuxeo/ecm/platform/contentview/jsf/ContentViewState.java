/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.contentview.jsf;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Implementations of this interface represent a content view state that can be
 * used to restore a given content view.
 * <p>
 * State is restricted to given getters and setters. Some state information is
 * actually taken on the page provider.
 *
 * @since 5.4.2
 */
public interface ContentViewState extends Serializable {

    String getContentViewName();

    void setContentViewName(String contentViewName);

    Long getPageSize();

    void setPageSize(Long pageSize);

    Long getCurrentPage();

    void setCurrentPage(Long currentPage);

    Object[] getQueryParameters();

    void setQueryParameters(Object[] parameters);

    DocumentModel getSearchDocumentModel();

    void setSearchDocumentModel(DocumentModel searchDocument);

    List<SortInfo> getSortInfos();

    void setSortInfos(List<SortInfo> sortInfos);

    ContentViewLayout getResultLayout();

    void setResultLayout(ContentViewLayout resultLayout);

    List<String> getResultColumns();

    void setResultColumns(List<String> resultColumns);

}
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
package org.nuxeo.ecm.platform.contentview.seam;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewStateImpl;
import org.nuxeo.ecm.platform.contentview.json.JSONContentViewState;

/**
 * Restful actions for save and restore of a content view
 *
 * @since 5.4.2
 */
@Name("contentViewRestActions")
@Scope(EVENT)
public class ContentViewRestActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected ContentViewService contentViewService;

    @In(create = true, required = true)
    protected transient CoreSession documentManager;

    public String getContentViewState(ContentView contentView)
            throws UnsupportedEncodingException, ClientException {
        ContentViewState state = contentViewService.saveContentView(contentView);
        if (state != null) {
            return JSONContentViewState.toJSON(state, true);
        }
        return null;
    }

    public ContentView restoreContentView(String contentViewName,
            Long currentPage, Long pageSize, List<SortInfo> sortInfos,
            String jsonContentViewState) throws UnsupportedEncodingException,
            ClientException {
        ContentViewState state = null;
        if (jsonContentViewState != null
                && jsonContentViewState.trim().length() != 0) {
            state = JSONContentViewState.fromJSON(jsonContentViewState, true,
                    documentManager);
        } else if (contentViewName != null) {
            // restore only from name
            state = new ContentViewStateImpl();
            state.setContentViewName(contentViewName);
        }
        if (state != null) {
            // apply current page and page size when set
            if (currentPage != null && currentPage.longValue() != -1) {
                state.setCurrentPage(currentPage);
            }
            if (pageSize != null && pageSize.longValue() != -1) {
                state.setPageSize(pageSize);
            }
            if (sortInfos != null) {
                state.setSortInfos(sortInfos);
            }
        }
        return contentViewService.restoreContentView(state, documentManager);
    }

    public List<SortInfo> getSortInfos(String sortColumn, boolean ascending) {
        List<SortInfo> sortInfos = new ArrayList<SortInfo>();
        sortInfos.add(new SortInfo(sortColumn, ascending));
        return sortInfos;
    }

}
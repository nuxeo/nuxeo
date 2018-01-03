/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.seam;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
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

    public String getContentViewState(ContentView contentView) throws IOException {
        ContentViewState state = contentViewService.saveContentView(contentView);
        if (state != null) {
            return JSONContentViewState.toJSON(state, true);
        }
        return null;
    }

    public ContentView restoreContentView(String contentViewName, Long currentPage, Long pageSize,
            List<SortInfo> sortInfos, String jsonContentViewState) throws IOException {
        ContentViewState state = null;
        if (jsonContentViewState != null && jsonContentViewState.trim().length() != 0) {
            state = JSONContentViewState.fromJSON(jsonContentViewState, true);
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
        return contentViewService.restoreContentView(state);
    }

    public List<SortInfo> getSortInfos(String sortColumn, boolean ascending) {
        List<SortInfo> sortInfos = new ArrayList<SortInfo>();
        sortInfos.add(new SortInfo(sortColumn, ascending));
        return sortInfos;
    }

}

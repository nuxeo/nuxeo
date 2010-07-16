/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.contentview;

import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for the content view object.
 * <p>
 * Provides simple getters for attributes defined in the XMap descriptor,
 * except cache key which is computed from currrent {@link FacesContext}
 * instance if cache key is an EL expression.
 * <p>
 * The page provider is initialized calling
 * {@link ContentViewService#getPageProvider(String, Object...)}.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class ContentViewImpl implements ContentView {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ContentViewImpl.class);

    protected String name;

    protected PageProvider<?> pageProvider;

    protected String selectionList;

    protected String pagination = "default";

    protected List<String> actionCategories;

    protected String searchLayoutName;

    protected String resultLayoutName;

    protected String cacheKey;

    protected List<String> refreshEventNames;

    public ContentViewImpl(String name, String selectionList,
            String pagination, List<String> actionCategories,
            String searchLayoutName, String resultLayoutName, String cacheKey,
            List<String> refreshEventNames) {
        super();
        this.name = name;
        this.selectionList = selectionList;
        this.pagination = pagination;
        this.actionCategories = actionCategories;
        this.searchLayoutName = searchLayoutName;
        this.resultLayoutName = resultLayoutName;
        this.cacheKey = cacheKey;
        this.refreshEventNames = refreshEventNames;
    }

    public String getName() {
        return name;
    }

    public String getSelectionListName() {
        return selectionList;
    }

    public String getPagination() {
        return pagination;
    }

    public List<String> getActionsCategories() {
        return actionCategories;
    }

    public String getSearchLayoutName() {
        return searchLayoutName;
    }

    public String getResultLayoutName() {
        return resultLayoutName;
    }

    public PageProvider<?> getPageProvider(Object... params)
            throws ClientException {
        if (pageProvider == null) {
            try {
                // make the service build the provider
                ContentViewService service = Framework.getService(ContentViewService.class);
                if (service == null) {
                    throw new ClientException(
                            "Could not resolve ContentViewService");
                }
                pageProvider = service.getPageProvider(getName(), params);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return pageProvider;
    }

    public PageProvider<?> getCurrentPageProvider() {
        return pageProvider;
    }

    public void resetPageProvider() {
        pageProvider = null;
    }

    public void refreshPageProvider() {
        if (pageProvider != null) {
            pageProvider.refresh();
        }
    }

    public String getCacheKey() {
        FacesContext context = FacesContext.getCurrentInstance();
        Object value = ComponentTagUtils.resolveElExpression(context, cacheKey);
        if (value != null && !(value instanceof String)) {
            log.error(String.format("Error processing expression '%s', "
                    + "result is not a String: %s", cacheKey, value));
        }
        return (String) value;
    }

    public List<String> getRefreshEventNames() {
        return refreshEventNames;
    }

}

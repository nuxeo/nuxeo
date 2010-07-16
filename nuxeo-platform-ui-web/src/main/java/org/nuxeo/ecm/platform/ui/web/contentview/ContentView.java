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

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PageProvider;

/**
 * A content view is a notion to handle lists of objects rendering, as well as
 * query filters to build the list.
 * <p>
 * It has a name that will be the resulting page provider name too. It handles
 * a page provider and accepts configuration needed to handle rendering, like
 * the search layout (for filtering options), the result layout (for results
 * rendering), actions (for buttons available when selecting result objects),
 * the selection list name...
 * <p>
 * It also handles refresh or reset of its provider, depending on its cache key
 * and refresh events configuration.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface ContentView extends Serializable {

    /**
     * Returns the name of this content view
     */
    String getName();

    /**
     * Returns the selection list name
     */
    String getSelectionListName();

    /**
     * Returns the pagination type to be used in pagination rendering
     */
    String getPagination();

    /**
     * Return the list of action categories to display buttons available on
     * selection of items.
     */
    List<String> getActionsCategories();

    /**
     * Returns the search layout name, used to filter results.
     */
    String getSearchLayoutName();

    /**
     * Returns the result layout name, used to display results.
     */
    String getResultLayoutName();

    /**
     * Returns the cache key for this provider, resolving from the current
     * {@link FacesContext} instance if it's an EL expression.
     */
    String getCacheKey();

    /**
     * Returns the list of event names that wshould trigger a refresh of this
     * content view page provider.
     */
    List<String> getRefreshEventNames();

    /**
     * Gets page provider according to given parameters
     *
     * @param params
     */
    PageProvider<?> getPageProvider(Object... params) throws ClientException;

    /**
     * Returns the current page provider, or null if
     * {@link #getPageProvider(Object...)} was never called before.
     */
    PageProvider<?> getCurrentPageProvider();

    /**
     * Resets the page provider.
     * <p>
     * A new page provider will be computed next time
     * {@link #getPageProvider(Object...)} is called.
     */
    void resetPageProvider();

    /**
     * Refreshes the current page provider if not null, see
     * {@link PageProvider#refresh()}
     */
    void refreshPageProvider();

}

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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PageProvider;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface ContentView extends Serializable {

    String getName();

    String getSelectionListName();

    String getPagination();

    List<String> getActionsCategories();

    String getSearchLayoutName();

    String getResultLayoutName();

    String getCacheKey();

    List<String> getRefreshEventNames();

    /**
     * Gets page provider according to given parameters
     *
     * @param params
     */
    PageProvider<?> getPageProvider(Object... params) throws ClientException;

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

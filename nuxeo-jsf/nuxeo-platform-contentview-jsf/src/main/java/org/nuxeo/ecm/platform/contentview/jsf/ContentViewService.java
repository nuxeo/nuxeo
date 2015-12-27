/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.contentview.jsf;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Service handling content views and associated page providers.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface ContentViewService extends Serializable {

    /**
     * Returns the content view with given name, or null if not found.
     *
     */
    ContentView getContentView(String name);

    /**
     * Returns the content view header, or null if not found.
     */
    ContentViewHeader getContentViewHeader(String name);

    /**
     * Returns all the registered content view names, or an empty set if no content view is registered.
     */
    Set<String> getContentViewNames();

    /**
     * Returns all the registered content view headers, or an empty set if no content view is registered.
     */
    Set<ContentViewHeader> getContentViewHeaders();

    /**
     * Returns all the registered content view names with given flag declared on their definition
     */
    Set<String> getContentViewNames(String flag);

    /**
     * Returns all the registered content view headers with given flag declared on their definition
     *
     * @since 5.4.2
     */
    Set<ContentViewHeader> getContentViewHeaders(String flag);

    /**
     * Returns the page provider computed from the content view with given name. Its properties are resolved using
     * current {@link FacesContext} instance if they are EL Expressions.
     * <p>
     * If not null, parameters sortInfos and pageSize will override information computed in the XML file. If not null,
     * currentPage will override default current page (0).
     *
     * @since 5.7
     */
    PageProvider<?> getPageProvider(String contentViewName, List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            DocumentModel searchDocument, Object... parameters);

    /**
     * Returns the state of this content view.
     * <p>
     * This state can be used to restore the content view in another context.
     *
     * @see #restoreContentView(ContentViewState)
     * @since 5.4.2
     * @param contentView
     */
    ContentViewState saveContentView(ContentView contentView);

    /**
     * Restores a content view given a state.
     *
     * @see #saveContentView(ContentView)
     * @since 5.4.2
     */
    ContentView restoreContentView(ContentViewState contentViewState);

    /**
     * Restores a content view to a given state.
     *
     * @since 7.3
     */
    void restoreContentViewState(ContentView contentView, ContentViewState contentViewState);

}

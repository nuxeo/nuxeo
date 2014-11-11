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
package org.nuxeo.ecm.platform.contentview.jsf;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
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
     * @throws ClientException
     * @deprecated: use {@link #getContentView(String, CoreSession)} instead
     */
    @Deprecated
    ContentView getContentView(String name) throws ClientException;

    /**
     * Returns the content view with given name, or null if not found.
     * <p>
     * If the content view is using a provider that needs a search document
     * model, a new one is created and attached to it thanks to the document
     * type held in the definition and to the core session.
     *
     * @param coreSession: a core session, used to initialize the search
     *            document model
     * @throws ClientException
     */
    ContentView getContentView(String name, CoreSession coreSession)
            throws ClientException;

    /**
     * Returns all the registered content view names, or an empty set if no
     * content view is registered.
     */
    Set<String> getContentViewNames();

    /**
     * Returns all the registered content view names with given flag declared
     * on their definition
     */
    Set<String> getContentViewNames(String flag);

    /**
     * Returns the page provider computed from the content view with given
     * name. Its properties are resolved using current {@link FacesContext}
     * instance if they are EL Expressions.
     * <p>
     * If not null, parameters sortInfos and pageSize will override information
     * computed in the XML file. If not null, currentPage will override default
     * current page (0).
     *
     * @throws ClientException
     */
    PageProvider<?> getPageProvider(String contentViewName,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Object... parameters) throws ClientException;

    /**
     * Returns the state of this content view.
     * <p>
     * This state can be used to restore the content view in another context.
     *
     * @see #restoreContentView(ContentViewState, CoreSession)
     * @since 5.4.2
     * @param contentView
     */
    ContentViewState saveContentView(ContentView contentView);

    /**
     * Restores a content view given a state.
     * <p>
     * The core session is only useful when restoring a content view defining a
     * page provider mapped to a document. It is used to initialize this
     * document.
     *
     * @see #saveContentView(ContentView)
     * @since 5.4.2
     * @throws ClientException
     */
    ContentView restoreContentView(ContentViewState contentViewState,
            CoreSession coreSession) throws ClientException;

}

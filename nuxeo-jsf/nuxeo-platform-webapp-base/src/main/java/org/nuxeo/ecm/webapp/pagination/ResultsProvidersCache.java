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

package org.nuxeo.ecm.webapp.pagination;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.api.SortNotSupportedException;

/**
 * This is a controller interface for pagination. The methods are bound to
 * actions a user can perform on a pagination controls (next, previous)
 * displayed by pageNavigationControls.xhtml
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 * @deprecated use {@link ContentView} instances in conjunction with
 *             {@link PageProvider} instead.
 */
@Deprecated
public interface ResultsProvidersCache {

    /**
     * Get a named results provider.
     * <p>
     * This handles caching and first instantiation.
     *
     * @throws ClientException
     * @throws SortNotSupportedException
     */
    PagedDocumentsProvider get(String name) throws ClientException;

    /**
     * Gets a named results provider.
     * <p>
     * This handles caching and first instantiation.
     *
     * @throws ClientException
     * @throws SortNotSupportedException
     */
    PagedDocumentsProvider get(String name, SortInfo sortInfo)
            throws ClientException, SortNotSupportedException;

    /**
     * Invalidates a results provider.
     * <p>
     * A new provider will be computed next time get() is called.
     */
    void invalidate(String name);

    /*
     * general Seam required methods
     */

    /**
     * Declaration for [Seam]Create method.
     */
    void init();

    /**
     * Declaration for [Seam]Destroy method.
     */
    void destroy();

    // TEMPORARY COMPATIBILITY
    int getNumberOfPages();

    int getPageIndex();

}

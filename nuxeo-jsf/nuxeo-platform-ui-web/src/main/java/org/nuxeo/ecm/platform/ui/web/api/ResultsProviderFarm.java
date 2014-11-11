/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;

/**
 * A Results Provider Farm registers methods to create named
 * {@link PagedDocumentsProvider}.
 * <p>
 * Typically, results provider implementations can vary a lot: we'll have the
 * search service, the document repository. This interface allows to register
 * and call factories for them.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 * @deprecated use ContentView instances in conjunction with
 *             PageProvider instead.
 */
@Deprecated
public interface ResultsProviderFarm {

    /**
     * Makes a new, fresh instance of the named results provider.
     *
     * @param name the name of the ResultsProvider instance to create
     * @return a PagedDocumentsProvider
     */
    // TODO: remake an distinction between malformed queries and internal
    // errors in the process/server that honors them
    PagedDocumentsProvider getResultsProvider(String name)
            throws ClientException, ResultsProviderFarmUserException;

    /**
     * @param name the name of the ResultsProvider instance to create
     * @param sortInfo an object containing the sort details
     */
    PagedDocumentsProvider getResultsProvider(String name, SortInfo sortInfo)
            throws ClientException, ResultsProviderFarmUserException;

}

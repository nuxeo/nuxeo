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

package org.nuxeo.ecm.webapp.querymodel;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;

/**
 * Action listener dedicated to the session management QueryModels.
 *
 * <p>QueryModels are non
 * persistent structures to build advanced NXQueries by incrementally editing
 * fields of a model that will be translated into search criteria once the
 * search is actually performed.</p>
 *
 * <p>This bean extends {@link ResultsProviderFarm} for <emph>stateful</emph>
 * Query Models <emph>only</emph>. Stateless models have to be handled by \]
 * another farm, which typically would still call the present bean to get the
 * QueryModel instance.</p>
 *
 *
 * @author Olivier Grisel (ogrisel@nuxeo.com)
 */

public interface QueryModelActions extends ResultsProviderFarm {

    boolean isInitialized();

    /**
     * Obtain a scoped instance of QueryModel.
     *
     * @param queryModelName
     * @return the
     * @throws ClientException
     */
    QueryModel get(String queryModelName) throws ClientException;

    void reset(String queryModelName) throws ClientException;

    /**
     * Observer on seam event to perform some necessary invalidations
     *
     * @param qm the query model that's been changed
     * @throws ClientException
     */
    void queryModelChanged(QueryModel qm) throws ClientException;

}

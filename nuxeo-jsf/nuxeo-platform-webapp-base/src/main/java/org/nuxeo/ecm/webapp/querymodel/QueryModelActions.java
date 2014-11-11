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

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;

/**
 * Action listener dedicated to the session management QueryModels.
 * <p>
 * QueryModels are non persistent structures to build advanced NXQueries by incrementally editing
 * fields of a model that will be translated into search criteria once the
 * search is actually performed.
 *
 * <p>
 * This bean extends {@link ResultsProviderFarm} for <emph>stateful</emph>
 * Query Models <emph>only</emph>. Stateless models have to be handled by \]
 * another farm, which typically would still call the present bean to get the
 * QueryModel instance.
 *
 * @author Olivier Grisel (ogrisel@nuxeo.com)
 */
public interface QueryModelActions extends ResultsProviderFarm {

    boolean isInitialized();

    /**
     * Obtain a scoped instance of QueryModel.
     */
    QueryModel get(String queryModelName) throws ClientException;

    /**
     * Save the specified <strong>stateful</strong> QueryModel.
     * <p>This is equivalent to the other signature, with saveSession set to True</p>
     *
     * @since 5.2
     * @param queryModelName
     * @param parentPath The path of parent folder to save into
     * @param name The local name to use.
     * @return The saved query model.
     * @throws ClientException forwarding from the Core, stateless QueryModel,
     *         if QueryModel has already been persisted
     */
    QueryModel persist(String queryModelName, String parentPath, String name) throws ClientException;

    /**
     * Save the specified <strong>stateful</strong> QueryModel.
     * <p>
     * The DocumentModel instance that backs the QueryModel is saved
     * in the CoreSession available in Seam's context. Necessary updates
     * are performed. The returned QueryModel instance is identical to the
     * one obtained by a subsequent call to {@see get}.
     * <p>
     * Further document operations, e.g., modifications, for this QueryModel
     * can be done on the DocumentModel instance directly, but CoreSession methods
     * returning a new DocumentModel instance must be followed by a call to
     * {@see load} and dependent objects (page providers, etc.) must be updated
     * as well.
     * <p>
     * It is not possible to call again this method on the same QueryModel, to
     * avoid consistency problems at the DocumentModel level. Therefore
     * If one wants to save a QueryModel, then change and eventually save it to
     * a different target in Nuxeo Core, one <strong>must</strong> call the
     * {@see reset} method before performing the changes.
     *
     * @since 5.2
     * @param queryModelName
     * @param parentPath The path of parent folder to save into
     * @param name The local name to use.
     * @param saveSession if true, the Core Session is saved
     * @return The saved query model.
     * @throws ClientException forwarding from the Core, stateless QueryModel,
     *         if QueryModel has already been persisted
     */
    QueryModel persist(String queryModelName, String parentPath, String name, boolean saveSession) throws ClientException;

    /**
     * Load a stateful QueryModel from the specified DocumentRef.
     * <p>The queryModelChanged event is thrown, meaning in particular that
     * {@link ResultsProviderCache} invalidation is performed.</p>
     * @since 5.2
     * @param queryModelName
     * @param ref
     * @return The loaded QueryModel instance.
     * @throws ClientException if qm is not stateful, document could not be retrieved.
     */
    QueryModel load(String queryModelName, DocumentRef ref) throws ClientException;

    /**
     * Tell if the DocumentModel behind the specified QueryModel has been
     * persisted in Nuxeo Core.
     * @since 5.2
     * @param queryModelName The query model name
     */
    boolean isPersisted(String queryModelName) throws ClientException;

    /**
     * Reset the specified QueryModel.
     * <p>Start over with a fresh,
     * transient DocumentModel instance.
     * </p>
     *
     * @param queryModelName
     * @throws ClientException
     */
    void reset(String queryModelName) throws ClientException;

    /**
     * Observer on Seam event to perform some necessary invalidations
     *
     * @param qm the query model that's been changed
     */
    void queryModelChanged(QueryModel qm);

}

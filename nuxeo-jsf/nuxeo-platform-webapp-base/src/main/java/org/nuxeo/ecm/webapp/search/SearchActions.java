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

package org.nuxeo.ecm.webapp.search;

import java.util.List;

import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @deprecated use {@link DocumentSearchActions} and content views instead
 */
@Deprecated
public interface SearchActions extends StatefulBaseLifeCycle,
        SelectDataModelListener, ResultsProviderFarm {

    String SEARCH_DOCUMENT_LIST = "SEARCH_DOCUMENT_LIST";

    String QM_ADVANCED = "ADVANCED_SEARCH";

    String QM_SIMPLE = "SIMPLE_SEARCH";

    String PROV_NXQL = "SEARCH_BEAN_NXQL_PROVIDER";

    /**
     * Declaration for [Seam]Create method.
     */
    void init();

    void destroy();

    /**
     * @return the query text - to be used in quick search form
     */
    String getSimpleSearchKeywords();

    void setSimpleSearchKeywords(String k);

    /**
     * @return the nxql query text if the option for nxql is selected
     */
    String getNxql();

    void setNxql(String nxql);

    String getSearchTypeId();

    void setSearchTypeId(String type);

    String getQueryErrorMsg();

    void setQueryErrorMsg(String msg);

    /**
     * @return the current targeted reindexation path
     */
    String getReindexPath();

    /**
     * Sets the current targeted reindexation path.
     */
    void setReindexPath(String path);

    List<DocumentModel> getResultDocuments(String providerName)
            throws ClientException;

    // action methods

    /**
     * Request dispatcher. Normally it will be called from a action link.
     */
    String search();

    void resetSearchField();

    String performSearch();

    String getDocumentLocation(DocumentModel doc);

    SelectDataModel getResultsSelectModel(String providerName)
            throws ClientException;

    /**
     * @return the Document Model backing the advanced search form up
     * @throws ClientException
     */
    DocumentModel getDocumentModel() throws ClientException;

    /**
     * Reindex all documents.
     *
     * @throws ClientException
     * @deprecated does nothing
     */
    @Deprecated
    void reindexDocuments() throws ClientException;

    /**
     * Reindex all documents under given path (inclusive).
     *
     * @param path
     * @throws ClientException
     * @deprecated does nothing
     */
    @Deprecated
    void reindexDocuments(String path) throws ClientException;

    /**
     * Reset the query fields.
     *
     * @return
     * @throws ClientException
     */
    String reset() throws ClientException;

    /**
     * This will be called with Seam remoting...
     *
     * @param docRef
     * @param selection
     * @return
     * @throws ClientException
     */
    @WebRemote
    String processSelectRow(String docRef, String providerName,
            Boolean selection) throws ClientException;

    /**
     * @return the latest used NXQL query from the scope (null if none)
     */
    String getLatestNxql();

    /**
     * Refresh cache
     */
    void refreshCache();

    /**
     * Is search service reindexing all ?
     *
     * @return bool flag
     */
    boolean isReindexingAll();

}

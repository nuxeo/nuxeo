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

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;

/**
 * This business delegate is available on session instance as a Seam component
 * and offers services to different action beans (like search action bean).
 * <p>
 * It delegates the calls to the server side and insure consistency over
 * multiple independent calls.
 *
 * @author DM
 */
@Name("searchDelegate")
@Scope(SESSION)
public class SearchBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 786391027463892L;

    private static final Log log = LogFactory.getLog(SearchBusinessDelegate.class);

    // need to be required = false since this is accessed also before connecting to a rep
    @In(create = true, required = false)
    private transient CoreSession documentManager;


    public DocumentModelList searchWithNXQL(String nxql)
            throws ClientException, SearchException {
        return searchWithNXQL(nxql, null);
    }

    /**
     * Performs an NXQL search for the given query and filter.
     *
     * @throws ClientException
     *             this can be thrown further to be intercepted
     * @throws SearchException
     *             this should be handled by the calling code
     */
    public DocumentModelList searchWithNXQL(String nxql, Filter filter)
            throws ClientException, SearchException {

        final String logPrefix = "<searchWithNXQL> ";

        log.debug(logPrefix + "NXQL: " + nxql);
        try {
            assert null != documentManager : "documentManager not injected from seam context";
            final DocumentModelList resultDocuments = documentManager.query(
                    nxql, filter);

            log.debug(logPrefix + "results contains " + resultDocuments.size()
                    + " docs");

            return resultDocuments;
        } catch (Throwable t) {
            final String cause = tryToExtractMeaningfulCause(t);
            if (cause != null) {
                throw new SearchException(cause);
            }

            throw ClientException.wrap(t);
        }
    }

    /**
     * Performs an NXQL search with limit for the given query.
     *
     * @throws ClientException
     *             this can be thrown further to be intercepted
     * @throws SearchException
     *             this should be handled by the calling code
     */
    public List<DocumentModel> searchWithNXQL(String nxql, Filter filter, int max)
            throws SearchException, ClientException {

        final String logPrefix = "<searchWithNXQL-limit> ";

        log.debug(logPrefix + "NXQL: " + nxql);
        try {
            assert null != documentManager : "documentManager not injected from seam context";
            final DocumentModelList resultDocuments = documentManager.query(
                    nxql, filter, max);

            log.debug(logPrefix + "results contains " + resultDocuments.size()
                    + " docs");

            return resultDocuments;
        } catch (Throwable t) {
            final String cause = tryToExtractMeaningfulCause(t);
            if (cause != null) {
                throw new SearchException(cause);
            }

            throw ClientException.wrap(t);
        }
    }

    /**
     * Performs an NXQL search with limit for the given query.
     *
     * @throws ClientException
     *             this can be thrown further to be intercepted
     * @throws SearchException
     *             this should be handled by the calling code
     */
    public DocumentModelIterator pagedSearchWithNXQL(String nxql, Filter filter, int max)
            throws SearchException, ClientException {

        final String logPrefix = "<searchWithNXQL-limit> ";

        log.debug(logPrefix + "NXQL: " + nxql);
        try {
            assert null != documentManager : "documentManager not injected from seam context";
            final DocumentModelIterator resultDocuments = documentManager.queryIt(
                    nxql, filter, max);

            log.debug(logPrefix + "results contains " + resultDocuments.size()
                    + " docs");

            return resultDocuments;
        } catch (Throwable t) {
            final String cause = tryToExtractMeaningfulCause(t);
            if (cause != null) {
                throw new SearchException(cause);
            }

            throw ClientException.wrap(t);
        }
    }

    /**
     * Searches the repository by the given keywords.
     * <p>
     * The result consists of a list of DocumentModel that can be rendered to UI.
     *
     * @see org.nuxeo.ecm.webapp.search.SearchActions#searchForText(String)
     */
    public DocumentModelList searchForText(String keywords)
            throws ClientException, SearchException {
        return searchForText(keywords, null);
    }

    /**
     * Searches the repository by the given keywords.
     * <p>
     * The result consists of a list of DocumentModel that can be rendered to UI.
     *
     * @return a list of DocumentModels (a DocumentModelList) matching the request.
     *
     * @see org.nuxeo.ecm.webapp.search.SearchActions#searchForText(java.lang.String)
     */
    public DocumentModelList searchForText(String keywords, Filter filter)
            throws ClientException, SearchException {

        final String logPrefix = "<searchForText> ";

        try {
            log.debug(logPrefix
                    + "Making call to get documents list for keywords: "
                    + keywords);
            assert null != documentManager : "documentManager not injected from Seam context";
            final DocumentModelList resultDocuments = documentManager
                    .querySimpleFts(keywords, filter);

            log.debug(logPrefix + "result contains: " + resultDocuments.size()
                    + " docs");

            return resultDocuments;
        } catch (Throwable t) {
            final String cause = tryToExtractMeaningfulCause(t);
            if (cause != null) {
                throw new SearchException(cause);
            }

            throw ClientException.wrap(t);
        }
    }

    /**
     * Searches the repository by the given keywords.
     *
     * @return DocumentModelIterator lazy loading iterator over the result containing DocumentModel objects
     *
     * @see org.nuxeo.ecm.webapp.search.SearchActions#searchForText(java.lang.String)
     */
    public DocumentModelIterator pagedSearchForText(String keywords, Filter filter, int pageSize)
            throws ClientException, SearchException {

        final String logPrefix = "<pagedSearchForText> ";

        try {
            log.debug(logPrefix
                    + "Making call to get documents list for keywords: "
                    + keywords);
            assert null != documentManager : "documentManager not injected from seam context";
            final DocumentModelIterator resultDocuments = documentManager
                    .querySimpleFtsIt(keywords, filter, pageSize);

            log.debug(logPrefix + "result contains: " + resultDocuments.size()
                    + " docs");

            return resultDocuments;
        } catch (Throwable t) {
            final String cause = tryToExtractMeaningfulCause(t);
            if (cause != null) {
                throw new SearchException(cause);
            }

            throw ClientException.wrap(t);
        }
    }

    // FIXME: add limit
    public List<DocumentModel> searchForText(String keywords,
            Filter filter,
            int maxResultsCount) throws SearchException, ClientException {

        final String logPrefix = "<searchForText-limit> ";

        try {
            log.debug(logPrefix
                    + "Making call to get documents list for keywords: "
                    + keywords);
            assert null != documentManager : "documentManager not injected from seam context";
            final DocumentModelList resultDocuments = documentManager
                    .querySimpleFts(keywords, filter);

            log.debug(logPrefix + "result contains: " + resultDocuments.size()
                    + " docs");

            return resultDocuments;
        } catch (Throwable t) {
            final String cause = tryToExtractMeaningfulCause(t);
            if (cause != null) {
                throw new SearchException(cause);
            }

            throw ClientException.wrap(t);
        }
    }

    /**
     * Utility method to extract meaningful error message from the given nesting
     * exception.
     *
     * TODO : maybe put it in NXCommon
     */
    private static String tryToExtractMeaningfulCause(Throwable t) {
        if (t instanceof ClientException) {

            // we have a Client Exception, but search even deeply for the original
            // message, if not found we return the message of this ClientException
            if (t.getCause() != null) {
                final String parentCauseMsg = ExceptionUtils.getRootCause(t)
                        .getMessage();
                if (parentCauseMsg != null
                        && parentCauseMsg.trim().length() > 0) {
                    return parentCauseMsg;
                }
            }

            return t.getMessage();
        }

        if (t.getCause() != null) {
            return tryToExtractMeaningfulCause(t.getCause());
        }

        return null;
    }

    /**
     * Creates a logical path for the given DocumentModel.
     */
    public String getDocLocation(DocumentModel doc) {
        assert doc != null;
        DocumentRef parentRef = doc.getParentRef();
        final Object[] titles;
        try {
            titles = documentManager.getDataModelsFieldUp(parentRef,
                    "dublincore", "title");
        } catch (ClientException e) {
            // TODO: more robust exception handling?
            log.error(e);
            return null;
        }

        final StringBuilder location = new StringBuilder();
        // the last item in the list is the title of the root which is null
        // and we build the path from final to start
        for (int i = titles.length - 2; i >= 0; i--) {
            Object title = titles[i];
            location.append('/');
            location.append(title); // should be string
        }

        if (titles.length == 1) {
            location.append('/'); // first level element
        }

        return location.toString();
    }

}

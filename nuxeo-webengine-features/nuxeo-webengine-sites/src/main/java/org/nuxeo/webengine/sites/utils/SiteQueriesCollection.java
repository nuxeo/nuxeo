/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     rdarlea
 */

package org.nuxeo.webengine.sites.utils;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;

/**
 * Collection of the queries used in the sites module. It gathers all in one
 * place to ease the reading of the code.
 *
 * @author rux
 */
public class SiteQueriesCollection {

    private SiteQueriesCollection() {
    }

    /**
     * Queries sites by URL and document type. It should be exactly one
     * returned.
     */
    public static DocumentModelList querySitesByUrlAndDocType(
            CoreSession session, final String url, String documentType)
            throws ClientException {
        String queryString = String.format("SELECT * FROM %s WHERE "
                + "ecm:mixinType = 'WebView' AND "
                + "ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
                + "AND ecm:currentLifeCycleState != 'deleted' "
                + "AND webc:isWebContainer = 1", documentType);

        return session.query(queryString, new Filter() {
            private static final long serialVersionUID = 259658360650139844L;

            public boolean accept(DocumentModel docModel) {
                try {
                    String webcUrl = (String) docModel.getPropertyValue("webc:url");
                    if ( webcUrl != null ) {
                        String encodedUrl = URIUtils.quoteURIPathComponent(url, false);
                        return webcUrl.equals(encodedUrl);
                    }
                    return false;
                } catch (Exception e) {
                    throw new ClientRuntimeException(e);
                }
            }
        });
    }

    /**
     * Queries sites unrestricted by URL and document type. It should be exactly
     * one returned.
     */
    public static boolean checkUnrestrictedSiteExistenceByUrlAndDocType(
            CoreSession session, String url, String documentType)
            throws ClientException {
        String queryString = String.format("SELECT * FROM %s WHERE "
                + "ecm:mixinType = 'WebView' AND webc:url = \"%s\" AND "
                + "ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
                + "AND ecm:currentLifeCycleState != 'deleted' "
                + "AND webc:isWebContainer = 1", documentType, url);
        UnrestrictedQueryRunner runner = new UnrestrictedQueryRunner(session,
                queryString);
        runner.runUnrestricted();
        return runner.getResultsSize() >= 1;
    }

    /**
     * Queries all sites of the given document type.
     */
    public static DocumentModelList queryAllSites(CoreSession session,
            String documentType) throws ClientException {
        String queryString = String.format("SELECT * FROM %s WHERE "
                + "ecm:mixinType = 'WebView' AND "
                + "ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
                + "AND ecm:currentLifeCycleState != 'deleted' "
                + "AND webc:isWebContainer = 1", documentType);
        return session.query(queryString);
    }

    /**
     * Queries the modified pages within a limit.
     */
    public static DocumentModelList queryLastModifiedPages(CoreSession session,
            String parent, String documentType, int numberLimit)
            throws ClientException {
        String queryString = String.format("SELECT * FROM %s WHERE "
                + "ecm:path STARTSWITH '%s' "
                + "AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
                + "AND ecm:currentLifeCycleState != 'deleted' "
                + "ORDER BY dc:modified DESC", documentType, parent);
        return session.query(queryString, null, numberLimit, 0, true);
    }

    /**
     * Queries the added comments within a limit. Query differs if moderated or
     * not.
     */
    public static DocumentModelList queryLastComments(CoreSession session,
            String parent, int numberLimit, boolean isModerated)
            throws ClientException {
        String queryString;
        if (isModerated) {
            queryString = String.format(
                    "SELECT * FROM Document WHERE "
                            + "ecm:primaryType like 'Comment' AND ecm:path STARTSWITH '%s' "
                            + "AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
                            + "AND ecm:currentLifeCycleState = '%s' "
                            + "ORDER BY dc:modified DESC", parent,
                    CommentsConstants.PUBLISHED_STATE);
        } else {
            queryString = String.format(
                    "SELECT * FROM Document WHERE "
                            + "ecm:primaryType like 'Comment' AND ecm:path STARTSWITH '%s' "
                            + "AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
                            + "AND ecm:currentLifeCycleState != 'deleted' "
                            + "ORDER BY dc:modified DESC", parent);
        }
        return session.query(queryString, null, numberLimit, 0, true);
    }

    /**
     * Queries the pages based on a search string.
     */
    public static DocumentModelList querySearchPages(CoreSession session,
            String query, String parent, String documentType, String dateAfter,
            String dateBefore) throws ClientException {
        StringBuilder queryString = new StringBuilder(String.format(
                "SELECT * FROM %s WHERE " + "ecm:path STARTSWITH  '%s' AND "
                        + "ecm:mixinType != 'HiddenInNavigation' AND "
                        + "ecm:isCheckedInVersion = 0 AND "
                        + "ecm:currentLifeCycleState != 'deleted'",
                documentType, parent));
        if (query != null) {
            queryString.append(String.format(" AND ecm:fulltext LIKE '%s' ",
                    query));
        }
        if (dateAfter != null && dateBefore != null) {
            queryString.append(String.format(
                    " AND dc:created BETWEEN DATE '%s' AND DATE '%s' ",
                    dateAfter, dateBefore));
        }
        return session.query(queryString.toString());
    }

    static class UnrestrictedQueryRunner extends UnrestrictedSessionRunner {

        DocumentModelList results;

        final String queryString;

        UnrestrictedQueryRunner(CoreSession session, String queryString) {
            super(session);
            this.queryString = queryString;
        }

        @Override
        public void run() throws ClientException {
            results = session.query(queryString);
        }

        public int getResultsSize() {
            return results.size();
        }
    }

}

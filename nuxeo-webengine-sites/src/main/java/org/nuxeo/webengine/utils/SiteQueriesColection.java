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

package org.nuxeo.webengine.utils;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;

/**
 * Collection of the queries used in the sites module. It gathers all in one
 * place to ease the reading of the code.
 * @author rux
 *
 */
public class SiteQueriesColection {
    
    /**
     * Queries sites by URL. It should be exactly one returned.
     * @param session
     * @param url
     * @return
     * @throws ClientException
     */
    public static DocumentModelList querySitesByUrl(CoreSession session, 
            String url) throws ClientException {
        String queryString = String.format("SELECT * FROM Document WHERE " + 
                "ecm:mixinType = 'WebView' AND webc:url = \"%s\" AND " +
                "ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 " +
                "AND ecm:currentLifeCycleState != 'deleted' " +
                "AND webc:isWebContainer = 1", url);
        return session.query(queryString);
    }

    /**
     * Queries all sites.
     * @param session
     * @return
     * @throws ClientException
     */
    public static DocumentModelList queryAllSites(CoreSession session) 
            throws ClientException {
        String queryString = "SELECT * FROM Document WHERE " + 
                "ecm:mixinType = 'WebView' AND " +
                "ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 " +
                "AND ecm:currentLifeCycleState != 'deleted' " +
                "AND webc:isWebContainer = 1";
        return session.query(queryString);
    }

    /**
     * Queries the modified pages within a limit.
     * @param session
     * @param parent
     * @param numberLimit
     * @return
     * @throws ClientException
     */
    public static DocumentModelList queryLastModifiedPages(CoreSession session,
            String parent, int numberLimit) throws ClientException {
        String queryString = String.format("SELECT * FROM Document WHERE " +
            "ecm:primaryType like 'WebPage' AND ecm:path STARTSWITH '%s' " +
            "AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 " +
            "AND ecm:currentLifeCycleState != 'deleted' " +
            "ORDER BY dc:modified DESC", parent);
        return session.query(queryString, null, numberLimit, 0, true);
    }
    
    /**
     * Queries the added comments within a limit. Query differs if moderated or not.
     * @param session
     * @param parent
     * @param numberLimit
     * @param isModerated
     * @return
     * @throws ClientException
     */
    public static DocumentModelList queryLastComments(CoreSession session,
            String parent, int numberLimit, boolean isModerated) throws ClientException {
        String queryString;
        if (isModerated) {
            queryString = String.format("SELECT * FROM Document WHERE " +
                    "ecm:primaryType like 'Comment' AND ecm:path STARTSWITH '%s' " +
                    "AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 " +
                    "AND ecm:currentLifeCycleState = '%s' "+
                    "ORDER BY dc:modified DESC", parent, CommentsConstants.PUBLISHED_STATE);
        } else {
            queryString = String.format("SELECT * FROM Document WHERE " +
                    "ecm:primaryType like 'Comment' AND ecm:path STARTSWITH '%s' " +
                    "AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 " +
                    "AND ecm:currentLifeCycleState != 'deleted' "+
                    "ORDER BY dc:modified DESC", parent);
        }
        return session.query(queryString, null, numberLimit, 0, true);
    }
    
    /**
     * Queries the pages based on a search string.
     * @param session
     * @param query
     * @param parent
     * @return
     * @throws ClientException
     */
    public static DocumentModelList querySearchPages(CoreSession session,
            String query, String parent) throws ClientException {
        String queryString = String.format("SELECT * FROM WebPage WHERE " +
                "ecm:fulltext LIKE '%s' AND ecm:path STARTSWITH  '%s' AND " +
                "ecm:mixinType != 'HiddenInNavigation' AND "+
                "ecm:isCheckedInVersion = 0 AND " +
                "ecm:currentLifeCycleState != 'deleted'", query, parent);
        return session.query(queryString);
    }
}

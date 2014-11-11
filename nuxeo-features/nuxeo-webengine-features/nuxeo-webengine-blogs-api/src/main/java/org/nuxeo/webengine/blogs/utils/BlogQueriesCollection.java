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
 */

package org.nuxeo.webengine.blogs.utils;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Collection of the queries used in the blogs module.
 */
public class BlogQueriesCollection {

    private BlogQueriesCollection() {
    }

    /**
     * Queries all blog posts within a blog site.
     */
    public static DocumentModelList getAllBlogPosts(CoreSession session,
            String parent) throws ClientException {
        String queryString = String.format("SELECT * FROM BlogPost WHERE "
                + "ecm:path STARTSWITH '%s' "
                + "AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
                + "AND ecm:currentLifeCycleState != 'deleted' "
                + "ORDER BY dc:modified ASC", parent);
        return session.query(queryString);
    }

}

/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.syndication.restAPI;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class DashBoardRestlet extends BaseQueryModelRestlet {

    @Override
    protected String getQueryModelName(Request req) {
        String qmName = (String) req.getAttributes().get("QMName");
        return qmName.toUpperCase();
    }

    @Override
    protected CoreSession getCoreSession(Request req, Response res, String repoName) {
        repoName = (String) (String)req.getResourceRef().getQueryAsForm().getFirstValue("repo");
        return super.getCoreSession(req, res, repoName);
    }

    @Override
    protected String getDefaultFormat(){
        return "ATOM";
    }

    protected String getDomainPath(Request req)
    {
           String domain = (String)req.getResourceRef().getQueryAsForm().getFirstValue("domain");
           if (domain==null) {
               domain = "/default-domain/";
           } else {
               if (!domain.startsWith("/")) {
                   domain = "/" + domain;
               }
               if (!domain.endsWith("/")) {
                   domain =  domain + "/";
               }
           }
           return domain;
    }

    @Override
    protected  List<String> extractQueryParameters(Request req) {
        List<String> queryParams = super.extractQueryParameters(req);

        String qmName = getQueryModelName(req);

        if ("USER_DOCUMENTS".equals(qmName)) {
            queryParams.add(0, getUserPrincipal(req).getName());
        }
        else if ("USER_DELETED_DOCUMENTS".equals(qmName)) {
            queryParams.add(0, getUserPrincipal(req).getName());
        }
        else if ("USER_WORKSPACES".equals(qmName)) {
            queryParams.add(0, getDomainPath(req));
        }
        else if ("USER_SITES".equals(qmName)) {
            queryParams.add(0, getDomainPath(req));
        }
        else if ("DOMAIN_PUBLISHED_DOCUMENTS".equals(qmName)) {
            queryParams.add(0, getDomainPath(req));
        }
        return queryParams;
    }

}

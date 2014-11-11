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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class DashBoardRestlet extends BaseQueryModelRestlet {

    private static final Log log = LogFactory.getLog(DashBoardRestlet.class);

    /**
     * Number of documents we pull from the audit, assuming after filtering
     * there will be enough left for the RELEVANT_DOCUMENTS querymodel.
     */
    // Could be a request parameter as well...
    public static final int RELEVANT_NUMBER = 30;

    @Override
    protected String getQueryModelName(Request req) {
        String qmName = (String) req.getAttributes().get("QMName");
        return qmName.toUpperCase();
    }

    @Override
    protected CoreSession getCoreSession(Request req, Response res,
            String repoName) {
        repoName = req.getResourceRef().getQueryAsForm().getFirstValue("repo");
        return super.getCoreSession(req, res, repoName);
    }

    @Override
    protected String getDefaultFormat() {
        return "ATOM";
    }

    protected String getDomainPath(Request req) {
        String domain = req.getResourceRef().getQueryAsForm().getFirstValue(
                "domain");
        if (domain == null) {
            domain = "/default-domain/";
        } else if (domain.equals("*")) {
            domain = "/";
        } else {
            if (!domain.startsWith("/")) {
                domain = "/" + domain;
            }
            if (!domain.endsWith("/")) {
                domain += "/";
            }
        }
        return domain;
    }

    /**
     * Finds the list of documents to use for the RELEVANT_DOCUMENTS querymodel.
     * <p>
     * This computes a short list of "relevant" documents for the user.
     */
    // TODO have different HQL queries depending on request params
    @SuppressWarnings("unchecked")
    protected List<String> getRelevantDocuments(Request req) {
        AuditReader auditReader;
        try {
            auditReader = Framework.getService(AuditReader.class);
        } catch (Exception e) {
            log.error("Cannot get AuditReader", e);
            return Collections.emptyList();
        }
        if (auditReader == null) {
            log.error("Cannot get AuditReader");
            return Collections.emptyList();
        }

        String username = getUserPrincipal(req).getName();
        String query = String.format(
                "select log.docUUID from LogEntry log" //
                        + " WHERE log.principalName = '%s'" //
                        + "   AND log.eventId IN" //
                        + "     ('%s', '%s')" //
                        + "   AND log.docLifeCycle IS NOT NULL" //
                        + "   AND log.docLifeCycle <> 'undefined'" //
                        + " ORDER BY log.eventDate DESC", //
                username, //
                DocumentEventTypes.DOCUMENT_CREATED,
                DocumentEventTypes.DOCUMENT_UPDATED);
        List<String> ids = (List) auditReader.nativeQuery(query, 1,
                RELEVANT_NUMBER);
        if (ids.isEmpty()) {
            // NXQL doesn't do IN (), so add dummy value
            ids.add("00000000-0000-0000-0000-000000000000");
        }
        return ids;
    }

    @Override
    protected List<Object> extractQueryParameters(Request req) {
        List<Object> queryParams = super.extractQueryParameters(req);

        String qmName = getQueryModelName(req);

        if ("USER_DOCUMENTS".equals(qmName)) {
            queryParams.add(0, getUserPrincipal(req).getName());
            queryParams.add(1, getDomainPath(req));
        } else if ("USER_DOMAINS".equals(qmName)) {
            // queryParams.add(0, getUserPrincipal(req).getName());
        } else if ("USER_DELETED_DOCUMENTS".equals(qmName)) {
            queryParams.add(0, getUserPrincipal(req).getName());
            // queryParams.add(1, getDomainPath(req));
        } else if ("USER_WORKSPACES".equals(qmName)) {
            queryParams.add(0, getDomainPath(req));
            queryParams.add(1, getDomainPath(req) + "templates");
        } else if ("USER_SITES".equals(qmName)) {
            queryParams.add(0, getDomainPath(req));
            queryParams.add(1, getDomainPath(req) + "templates");
        } else if ("DOMAIN_PUBLISHED_DOCUMENTS".equals(qmName)) {
            queryParams.add(0, getDomainPath(req));
        } else if ("DOMAIN_DOCUMENTS".equals(qmName)) {
            queryParams.add(0, getDomainPath(req));
            queryParams.add(1, getDomainPath(req) + "templates");
        } else if ("USER_SECTIONS".equals(qmName)) {
            queryParams.add(0, getDomainPath(req));
        } else if ("RELEVANT_DOCUMENTS".equals(qmName)) {
            queryParams.add(0, getRelevantDocuments(req));
        }

        return queryParams;
    }

}

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
 *     anguenot
 *
 * $Id: WSAudit.java 28230 2007-12-18 15:21:51Z rdarlea $
 */

package org.nuxeo.ecm.platform.audit.ws.api;

import org.nuxeo.ecm.platform.api.ws.BaseNuxeoWebService;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.ws.EventDescriptorPage;
import org.nuxeo.ecm.platform.audit.ws.ModifiedDocumentDescriptor;
import org.nuxeo.ecm.platform.audit.ws.ModifiedDocumentDescriptorPage;

/**
 * Audit Web service interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public interface WSAudit extends BaseNuxeoWebService {

    /**
     * Returns the list of modified document within a timestamp.
     *
     * @param sessionId : the Nuxeo core session id.
     * @paral dateRangeQuery : the date range query.
     * @return a list of modified document descriptors.
     * @throws AuditException
     */
    ModifiedDocumentDescriptor[] listModifiedDocuments(String sessionId,
            String dateRangeQuery) throws AuditException;

    ModifiedDocumentDescriptorPage listModifiedDocumentsByPage(
            String sessionId, String dateRangeQuery, String path, int page, int pageSize)
            throws AuditException;

    EventDescriptorPage listEventsByPage(String sessionId,
            String dateRangeQuery, int page, int pageSize)
            throws AuditException;

    EventDescriptorPage queryEventsByPage(String sessionId, String whereClause,
            int page, int pageSize) throws AuditException;

}

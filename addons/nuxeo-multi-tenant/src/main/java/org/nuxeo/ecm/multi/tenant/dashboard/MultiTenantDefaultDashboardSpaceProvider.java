/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant.dashboard;

import static org.nuxeo.ecm.user.center.dashboard.AbstractDashboardSpaceCreator.DASHBOARD_MANAGEMENT_NAME;
import static org.nuxeo.ecm.user.center.dashboard.AbstractDashboardSpaceCreator.DASHBOARD_MANAGEMENT_TYPE;
import static org.nuxeo.ecm.user.center.dashboard.DefaultDashboardSpaceCreator.DEFAULT_DASHBOARD_SPACE_NAME;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.multi.tenant.MultiTenantHelper;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.user.center.dashboard.DefaultDashboardSpaceProvider;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantDefaultDashboardSpaceProvider extends
        DefaultDashboardSpaceProvider {

    private static final Log log = LogFactory.getLog(MultiTenantDefaultDashboardSpaceProvider.class);

    protected Space getOrCreateSpace(CoreSession session,
            Map<String, String> parameters) throws ClientException {
        String tenantId = MultiTenantHelper.getCurrentTenantId(session.getPrincipal());

        if (StringUtils.isBlank(tenantId)) {
            return super.getOrCreateSpace(session, parameters);
        }

        String tenantDocumentPath = MultiTenantHelper.getTenantDocumentPath(
                session, tenantId);
        if (tenantDocumentPath == null) {
            return super.getOrCreateSpace(session, parameters);
        }

        PathRef tenantDashboardManagementRef = new PathRef(tenantDocumentPath,
                DASHBOARD_MANAGEMENT_NAME);
        if (session.exists(tenantDashboardManagementRef)) {
            DocumentRef spaceRef = new PathRef(
                    tenantDashboardManagementRef.toString(),
                    DEFAULT_DASHBOARD_SPACE_NAME);
            if (session.exists(spaceRef)) {
                DocumentModel existingSpace = session.getDocument(spaceRef);
                return existingSpace.getAdapter(Space.class);
            } else {
                DocumentRef defaultDashboardSpaceRef = getOrCreateDefaultDashboardSpace(
                        session, parameters, tenantId,
                        tenantDashboardManagementRef.toString());
                DocumentModel defaultDashboardSpace = session.getDocument(defaultDashboardSpaceRef);
                return defaultDashboardSpace.getAdapter(Space.class);
            }
        } else {
            DocumentModel tenantDashboardManagement = session.createDocumentModel(
                    tenantDocumentPath, DASHBOARD_MANAGEMENT_NAME,
                    DASHBOARD_MANAGEMENT_TYPE);
            tenantDashboardManagement.setPropertyValue("dc:title",
                    "Tenant dashboard management");
            tenantDashboardManagement.setPropertyValue("dc:description",
                    "Tenant dashboard management");
            tenantDashboardManagement = session.createDocument(tenantDashboardManagement);
            DocumentRef defaultDashboardSpaceRef = getOrCreateDefaultDashboardSpace(
                    session, parameters, tenantId,
                    tenantDashboardManagement.getPathAsString());
            DocumentModel defaultDashboardSpace = session.getDocument(defaultDashboardSpaceRef);
            return defaultDashboardSpace.getAdapter(Space.class);
        }

    }

    protected DocumentRef getOrCreateDefaultDashboardSpace(CoreSession session,
            Map<String, String> parameters, String tenantId,
            String tenantDashboardManagementPath) throws ClientException {
        MultiTenantDefaultDashboardSpaceCreator defaultDashboardSpaceCreator = new MultiTenantDefaultDashboardSpaceCreator(
                session, parameters, tenantId, tenantDashboardManagementPath);
        defaultDashboardSpaceCreator.runUnrestricted();
        return defaultDashboardSpaceCreator.defaultDashboardSpaceRef;
    }

}

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

import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;

import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.multi.tenant.MultiTenantHelper;
import org.nuxeo.ecm.user.center.dashboard.DefaultDashboardSpaceCreator;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantDefaultDashboardSpaceCreator extends
        DefaultDashboardSpaceCreator {

    protected String tenantId;

    protected String tenantDashboardManagementPath;

    public MultiTenantDefaultDashboardSpaceCreator(CoreSession session,
            Map<String, String> parameters, String tenantId,
            String tenantDashboardManagementPath) {
        super(session, parameters);
        this.tenantId = tenantId;
        this.tenantDashboardManagementPath = tenantDashboardManagementPath;
    }

    @Override
    public void run() throws ClientException {
        String defaultDashboardSpacePath = new Path(
                tenantDashboardManagementPath).append(
                DEFAULT_DASHBOARD_SPACE_NAME).toString();
        DocumentRef defaultDashboardSpacePathRef = new PathRef(
                defaultDashboardSpacePath);

        DocumentModel defaultDashboardSpace;
        if (!session.exists(defaultDashboardSpacePathRef)) {
            defaultDashboardSpace = createDefaultDashboardSpace(tenantDashboardManagementPath);
        } else {
            defaultDashboardSpace = session.getDocument(defaultDashboardSpacePathRef);
        }
        defaultDashboardSpaceRef = defaultDashboardSpace.getRef();
    }

    protected void addDefaultACP(DocumentModel defaultDashboardSpace)
            throws ClientException {
        ACP acp = defaultDashboardSpace.getACP();
        ACL acl = acp.getOrCreateACL();
        String tenantAdministratorsGroup = MultiTenantHelper.computeTenantAdministratorsGroup(tenantId);
        acl.add(new ACE(tenantAdministratorsGroup,
                SecurityConstants.EVERYTHING, true));
        acl.add(new ACE(EVERYONE, EVERYTHING, false));
        defaultDashboardSpace.setACP(acp, true);
    }

}

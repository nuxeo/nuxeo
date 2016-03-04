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

package org.nuxeo.ecm.multi.tenant.userworkspace;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.multi.tenant.MultiTenantHelper;
import org.nuxeo.ecm.multi.tenant.MultiTenantService;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;
import org.nuxeo.ecm.platform.userworkspace.core.service.DefaultUserWorkspaceServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Multi tenant aware implementation of the {@link org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService}.
 * <p>
 * If there is a current tenant, the UserWorkspaceRoot is stored inside the tenant, otherwise it uses the default
 * behavior of {@link DefaultUserWorkspaceServiceImpl}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantUserWorkspaceService extends DefaultUserWorkspaceServiceImpl {

    private static final long serialVersionUID = 1L;

    protected String getTenantId(CoreSession userCoreSession, String userName) throws ClientException {
        String tenantId = null;
        if (userName == null) {
            userName = userCoreSession.getPrincipal().getName();
        }
        MultiTenantService multiTenantService = Framework.getLocalService(MultiTenantService.class);
        if (multiTenantService.isTenantIsolationEnabled(userCoreSession)) {
            tenantId = MultiTenantHelper.getTenantId(userName);
        }
        return tenantId;
    }

    @Override
    protected String computePathUserWorkspaceRoot(CoreSession userCoreSession, String userName,
            DocumentModel currentDocument) throws ClientException {

        String tenantId = getTenantId(userCoreSession, userName);
        if (StringUtils.isBlank(tenantId)) {
            // default behavior
            return super.computePathUserWorkspaceRoot(userCoreSession, userName, currentDocument);
        } else {
            // tenant specific behavior
            return computePathUserWorkspaceRootForTenant(userCoreSession, tenantId);
        }
    }

    protected String computePathUserWorkspaceRootForTenant(CoreSession session, String tenantId)
            throws ClientException {
        String tenantDocumentPath = MultiTenantHelper.getTenantDocumentPath(session, tenantId);
        Path path = new Path(tenantDocumentPath);
        path = path.append(UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        return path.toString();
    }

}

/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant.userworkspace;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.Path;
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

    protected String getTenantId(CoreSession userCoreSession, String userName) {
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
            DocumentModel currentDocument) {

        String tenantId = getTenantId(userCoreSession, userName);
        if (StringUtils.isBlank(tenantId)) {
            // default behavior
            return super.computePathUserWorkspaceRoot(userCoreSession, userName, currentDocument);
        } else {
            // tenant specific behavior
            return computePathUserWorkspaceRootForTenant(userCoreSession, tenantId);
        }
    }

    protected String computePathUserWorkspaceRootForTenant(CoreSession session, String tenantId) {
        String tenantDocumentPath = MultiTenantHelper.getTenantDocumentPath(session, tenantId);
        Path path = new Path(tenantDocumentPath);
        path = path.append(UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        return path.toString();
    }


}

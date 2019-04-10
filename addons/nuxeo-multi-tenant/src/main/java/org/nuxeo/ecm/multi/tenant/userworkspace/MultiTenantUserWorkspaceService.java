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
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.multi.tenant.MultiTenantHelper;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;
import org.nuxeo.ecm.platform.userworkspace.core.service.DefaultUserWorkspaceServiceImpl;

/**
 * Multi tenant aware implementation of the
 * {@link org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService}.
 * <p>
 * If there is a current tenant, the UserWorkspaceRoot is stored inside the
 * tenant, otherwise it uses the default behavior of
 * {@link DefaultUserWorkspaceServiceImpl}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class MultiTenantUserWorkspaceService extends
        DefaultUserWorkspaceServiceImpl {

    private static final long serialVersionUID = 1L;

    @Override
    protected String computePathUserWorkspaceRoot(CoreSession userCoreSession,
            DocumentModel currentDocument) throws ClientException {        
        String tenantId = MultiTenantHelper.getCurrentTenantId(userCoreSession.getPrincipal());
        if (StringUtils.isBlank(tenantId)) {
            // default behavior
            return super.computePathUserWorkspaceRoot(userCoreSession,
                    currentDocument);
        }

        return computePathUserWorkspaceRoot(userCoreSession, tenantId);
    }

    protected String computePathUserWorkspaceRoot(CoreSession session,
            String tenantId) throws ClientException {
        String tenantDocumentPath = MultiTenantHelper.getTenantDocumentPath(
                session, tenantId);
        Path path = new Path(tenantDocumentPath);
        path = path.append(UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        return path.toString();
    }

    /**
     * Overridden to compute the right user workspace path for an user which is
     * not the current user in the {@code userCoreSession}.
     */
    @Override
    protected String computePathForUserWorkspace(CoreSession userCoreSession,
            String userName, DocumentModel currentDocument)
            throws ClientException {
        String tenantId = MultiTenantHelper.getTenantId(userName);
        if (StringUtils.isBlank(tenantId)) {
            // default behavior
            return super.computePathForUserWorkspace(userCoreSession, userName,
                    currentDocument);
        }

        return computePathForUserWorkspace(userCoreSession, tenantId, userName);
    }

    protected String computePathForUserWorkspace(CoreSession session,
            String tenantId, String userName) throws ClientException {
        String rootPath = computePathUserWorkspaceRoot(session, tenantId);
        Path path = new Path(rootPath);
        path = path.append(getUserWorkspaceNameForUser(userName));
        return path.toString();
    }

    /**
     * Overridden to get the right user workspace when getting / creating a user
     * workspace for a different user than the current user in the
     * {@code userCoreSession}.
     */
    @Override
    protected DocumentModel getCurrentUserPersonalWorkspace(String userName,
            CoreSession userCoreSession, DocumentModel context)
            throws ClientException {
        String tenantId = MultiTenantHelper.getTenantId(userName);
        if (StringUtils.isBlank(tenantId)) {
            // default behavior
            return super.getCurrentUserPersonalWorkspace(userName,
                    userCoreSession, context);
        }

        PathRef uwsDocRef = new PathRef(computePathForUserWorkspace(
                userCoreSession, tenantId, userName));
        if (!userCoreSession.exists(uwsDocRef)) {
            // do the creation
            PathRef rootRef = new PathRef(computePathUserWorkspaceRoot(
                    userCoreSession, tenantId));
            uwsDocRef = createUserWorkspace(rootRef, uwsDocRef,
                    userCoreSession, userName);
        }
        // force Session synchro to process invalidation (in non JCA cases)
        if (userCoreSession.getClass().getSimpleName().equals("LocalSession")) {
            userCoreSession.save();
        }
        return userCoreSession.getDocument(uwsDocRef);
    }

    /**
     * @deprecated since 5.7.2, not used anymore.
     */
    @Deprecated
    protected boolean isSameUserName(CoreSession session, String userName) {
        return session.getPrincipal().getName().equals(userName);
    }

}

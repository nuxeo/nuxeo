/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *  Implementation of {@code UserProfileService}.
 *
 * @see UserProfileService
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.4.3
 */
public class UserProfileServiceImpl extends DefaultComponent implements
        UserProfileService {

    private UserWorkspaceService userWorkspaceService;

    @Override
    public DocumentModel getUserProfileDocument(CoreSession session)
            throws ClientException {
        DocumentModel userWorkspace = getUserWorkspaceService().getCurrentUserPersonalWorkspace(
                session, null);
        return getOrCreateUserProfileDocument(session, userWorkspace);
    }

    @Override
    public DocumentModel getUserProfileDocument(String userName,
            CoreSession session) throws ClientException {
        DocumentModel userWorkspace = getUserWorkspaceService().getUserPersonalWorkspace(
                userName, session.getRootDocument());
        return getOrCreateUserProfileDocument(session, userWorkspace);
    }

    private DocumentModel getOrCreateUserProfileDocument(CoreSession session,
            DocumentModel userWorkspace) throws ClientException {
        DocumentModelList children = session.getChildren(
                userWorkspace.getRef(),
                UserProfileConstants.USER_PROFILE_DOCTYPE);
        DocumentModel userProfileDoc;
        if (!children.isEmpty()) {
            userProfileDoc = children.get(0);
        } else {
            userProfileDoc = session.createDocumentModel(
                    userWorkspace.getPathAsString(),
                    String.valueOf(System.currentTimeMillis()),
                    UserProfileConstants.USER_PROFILE_DOCTYPE);
            if (session.hasPermission(userWorkspace.getRef(),
                    SecurityConstants.ADD_CHILDREN)) {
                userProfileDoc = session.createDocument(userProfileDoc);
                ACP acp = session.getACP(userProfileDoc.getRef());
                ACL acl = acp.getOrCreateACL();
                acl.add(new ACE(SecurityConstants.EVERYONE,
                        SecurityConstants.READ, true));
                acp.addACL(acl);
                session.setACP(userProfileDoc.getRef(), acp, true);
                session.save();
            }
        }
        return userProfileDoc;
    }

    private UserWorkspaceService getUserWorkspaceService() {
        if (userWorkspaceService == null) {
            userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
        }
        return userWorkspaceService;
    }

}

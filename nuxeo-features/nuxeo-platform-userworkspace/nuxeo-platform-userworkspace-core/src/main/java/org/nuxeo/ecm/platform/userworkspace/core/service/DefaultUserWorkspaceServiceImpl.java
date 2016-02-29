/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.core.service;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;

/**
 * Default implementation of the {@link UserWorkspaceService}.
 *
 * @author tiry
 */
public class DefaultUserWorkspaceServiceImpl extends AbstractUserWorkspaceImpl implements UserWorkspaceService {

    private static final long serialVersionUID = 1L;

    protected String getUserWorkspaceRootType() {
        return getComponent().getConfiguration().getUserWorkspaceRootType();
    }

    protected String getUserWorkspaceType() {
        return getComponent().getConfiguration().getUserWorkspaceType();
    }

    protected void setUserWorkspaceRootACL(DocumentModel doc) {
        ACP acp = new ACPImpl();
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { denyEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);
    }

    protected void setUserWorkspaceACL(DocumentModel doc, String userName) {
        ACP acp = new ACPImpl();
        ACE grantEverything = new ACE(userName, SecurityConstants.EVERYTHING, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { grantEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);
    }

    @Override
    protected DocumentModel doCreateUserWorkspacesRoot(CoreSession unrestrictedSession, PathRef rootRef) {
        String parentPath = new Path(rootRef.toString()).removeLastSegments(1).toString();
        DocumentModel doc = unrestrictedSession.createDocumentModel(parentPath,
                UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT, getUserWorkspaceRootType());
        doc.setProperty("dublincore", "title", UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        doc.setProperty("dublincore", "description", "");
        doc = unrestrictedSession.createDocument(doc);

        setUserWorkspaceRootACL(doc);

        return doc;
    }

    @Override
    protected DocumentModel doCreateUserWorkspace(CoreSession unrestrictedSession, PathRef wsRef, Principal principal,
            String userName) {

        String parentPath = new Path(wsRef.toString()).removeLastSegments(1).toString();
        String wsName = new Path(wsRef.toString()).lastSegment();
        DocumentModel doc = unrestrictedSession.createDocumentModel(parentPath, wsName, getUserWorkspaceType());

        doc.setProperty("dublincore", "title", buildUserWorkspaceTitle(principal, userName));
        doc.setProperty("dublincore", "description", "");
        doc = unrestrictedSession.createDocument(doc);

        setUserWorkspaceACL(doc, userName);

        /**
         * @since 5.7
         */
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("username", userName);
        notifyEvent(unrestrictedSession, doc, (NuxeoPrincipal) unrestrictedSession.getPrincipal(),
                DocumentEventTypes.USER_WORKSPACE_CREATED, properties);
        return doc;
    }

}

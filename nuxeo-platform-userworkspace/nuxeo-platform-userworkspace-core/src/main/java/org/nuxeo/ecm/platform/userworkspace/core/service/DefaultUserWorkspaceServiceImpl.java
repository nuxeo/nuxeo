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

package org.nuxeo.ecm.platform.userworkspace.core.service;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
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
public class DefaultUserWorkspaceServiceImpl extends AbstractUserWorkspaceImpl
        implements UserWorkspaceService {

    private static final long serialVersionUID = 1L;
    
    protected String getUserWorkspaceRootType() {
        return getComponent().getConfiguration().getUserWorkspaceRootType();
    }
    
    protected String getUserWorkspaceType() {
        return getComponent().getConfiguration().getUserWorkspaceType();
    }

    protected void setUserWorkspaceRootACL( DocumentModel doc) throws ClientException {
        ACP acp = new ACPImpl();
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, false);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { denyEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);
    }

    protected void setUserWorkspaceACL( DocumentModel doc, String userName) throws ClientException {
        ACP acp = new ACPImpl();
        ACE grantEverything = new ACE(userName, SecurityConstants.EVERYTHING,
                true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { grantEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);
    }

    protected DocumentModel doCreateUserWorkspacesRoot(
            CoreSession unrestrictedSession, PathRef rootRef)
            throws ClientException {

        String parentPath = new Path(rootRef.toString()).removeLastSegments(1).toString();
        DocumentModel doc = unrestrictedSession.createDocumentModel(parentPath,
                UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT,
                getUserWorkspaceRootType());
        doc.setProperty("dublincore", "title",
                UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        doc.setProperty("dublincore", "description", "");
        doc = unrestrictedSession.createDocument(doc);

        setUserWorkspaceRootACL(doc);        

        return doc;
    }

    protected DocumentModel doCreateUserWorkspace(
            CoreSession unrestrictedSession, PathRef wsRef,
            Principal principal, String userName) throws ClientException {

        String parentPath = new Path(wsRef.toString()).removeLastSegments(1).toString();
        String wsName = new Path(wsRef.toString()).lastSegment();
        DocumentModel doc = unrestrictedSession.createDocumentModel(parentPath,
                wsName, getUserWorkspaceType());

        doc.setProperty("dublincore", "title",
                buildUserWorkspaceTitle(principal, userName));
        doc.setProperty("dublincore", "description", "");
        doc = unrestrictedSession.createDocument(doc);
                
        setUserWorkspaceACL(doc, userName);                
        
        /**
         * @since 5.7
         */
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("username", userName);
        notifyEvent(unrestrictedSession, doc,
                (NuxeoPrincipal) unrestrictedSession.getPrincipal(),
                DocumentEventTypes.USER_WORKSPACE_CREATED, properties);
        return doc;
    }

}

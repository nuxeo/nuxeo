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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;

/**
 *
 * Default implementation of the {@link UserWorkspaceService}
 *
 * @author tiry
 *
 */
public class DefaultUserWorkspaceServiceImpl implements UserWorkspaceService {

    private static final Log log = LogFactory
            .getLog(DefaultUserWorkspaceServiceImpl.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    // define target Domain Name
    protected String getDomainName(DocumentModel currentDocument) {
        if (currentDocument!=null) {
            return currentDocument.getPath().segment(0);
        }
        else {
            return UserWorkspaceServiceImplComponent.getTargetDomainName();
        }
    }

    // get the name of the UserWorkspace given the user name
    protected String getUserWorkspaceNameForUser(String userName) {
        return IdUtils.generateId(userName, "-", false, 30);
    }

    // compute the path of the Root of all userWorspaces
    protected String computePathUserWorkspaceRoot(DocumentModel currentDocument) {
        String domainName = getDomainName(currentDocument);
        Path path = new Path("/" + domainName);
        path=path.append(UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        return path.toString();
    }

    // compute the path of the userWorspace
    protected String computePathForUserWorkspace(String userName,
            DocumentModel currentDocument) {
        String rootPath = computePathUserWorkspaceRoot(currentDocument);
        Path path = new Path(rootPath);
        path=path.append(getUserWorkspaceNameForUser(userName));
        return path.toString();
    }

    public DocumentModel getCurrentUserPersonalWorkspace(String userName,
            DocumentModel currentDocument) throws ClientException {
        if (currentDocument == null) {
            return null;
        }
        CoreSession userCoreSession = CoreInstance.getInstance().getSession(
                currentDocument.getSessionId());

        return getCurrentUserPersonalWorkspace(userName, userCoreSession, currentDocument);
    }

    public DocumentModel getCurrentUserPersonalWorkspace(CoreSession userCoreSession, DocumentModel context) throws ClientException {
        return getCurrentUserPersonalWorkspace(userCoreSession.getPrincipal().getName(), userCoreSession, context);
    }

    protected DocumentModel getCurrentUserPersonalWorkspace(String userName, CoreSession userCoreSession, DocumentModel context) throws ClientException {

        PathRef uwsDocRef = new PathRef(computePathForUserWorkspace(
                userName, context));

        if (!userCoreSession.exists(uwsDocRef)) {
            // do the creation
            PathRef rootRef = new PathRef(
                    computePathUserWorkspaceRoot(context));
            createUserWorkspace(rootRef, uwsDocRef, userCoreSession, userName);
        }

        // force Session synchro to process invalidation (in non JCA cases)
        if (userCoreSession.getClass().getSimpleName().equals("LocalSession")) {
            userCoreSession.save();
        }

        return userCoreSession.getDocument(uwsDocRef);
    }

    protected synchronized void createUserWorkspace(PathRef rootRef,
            PathRef userWSRef, CoreSession userCoreSession, String userName)
            throws ClientException {
        new UnrestrictedUWSCreator(rootRef, userWSRef, userCoreSession, userName)
                .runUnrestricted();
    }

    protected class UnrestrictedUWSCreator extends UnrestrictedSessionRunner {

        private PathRef rootRef;
        private PathRef userWSRef;
        private String userName;

        public UnrestrictedUWSCreator(PathRef rootRef,
                PathRef userWSRef, CoreSession userCoreSession, String userName) {
            super(userCoreSession);
            this.rootRef = rootRef;
            this.userWSRef = userWSRef;
            this.userName=userName;
        }

        @Override
        public void run() throws ClientException {

            // create root if needed
            if (!session.exists(rootRef)) {
                DocumentModel root = createUserWorkspacesRoot(session, rootRef);
                assert(root.getPathAsString().equals(rootRef.toString()));
            }

            // create user WS if needed
            if (!session.exists(userWSRef)) {
                DocumentModel uw = createUserWorkspace(session, userWSRef, userName);
                assert(uw.getPathAsString().equals(userWSRef.toString()));
            }

            session.save();
        }

    }

    protected static DocumentModel createUserWorkspacesRoot(
            CoreSession unrestrictedSession, PathRef rootRef)
            throws ClientException {

        String parentPath = new Path(rootRef.toString()).removeLastSegments(1).toString();
        DocumentModel doc = unrestrictedSession.createDocumentModel(parentPath,
                UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT,"UserWorkspacesRoot");
        doc.setProperty("dublincore", "title",
                UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        doc.setProperty("dublincore", "description", "");
        doc = unrestrictedSession.createDocument(doc);

        ACP acp = new ACPImpl();
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, false);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { denyEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);

        return doc;
    }

    protected static DocumentModel createUserWorkspace(
            CoreSession unrestrictedSession, PathRef wsRef, String userName)
            throws ClientException {

        String parentPath = new Path(wsRef.toString()).removeLastSegments(1).toString();
        String wsName = new Path(wsRef.toString()).lastSegment();
        DocumentModel doc = unrestrictedSession.createDocumentModel(parentPath,
                wsName,"Workspace");
        doc.setProperty("dublincore", "title",
                userName);
        doc.setProperty("dublincore", "description", "");
        doc = unrestrictedSession.createDocument(doc);

        ACP acp = new ACPImpl();
        ACE grantEverything = new ACE(userName, SecurityConstants.EVERYTHING,
                true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { grantEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);

        return doc;
    }


}

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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of the {@link UserWorkspaceService}.
 *
 * @author tiry
 */
public class DefaultUserWorkspaceServiceImpl implements UserWorkspaceService {

    private static final Log log = LogFactory.getLog(DefaultUserWorkspaceServiceImpl.class);

    private static final long serialVersionUID = 1L;

    protected String targetDomainName;

    // try to use configured domain
    // use first domain available otherwise
    protected String getDomainName(CoreSession userCoreSession,
            DocumentModel currentDocument) {
        if (targetDomainName == null) {
            RootDomainFinder finder = new RootDomainFinder(userCoreSession);
            try {
                finder.runUnrestricted();
            } catch (ClientException e) {
                log.error("Unable to find root domain for UserWorkspace", e);
                return null;
            }
            targetDomainName = finder.domaineName;
        }
        return targetDomainName;
    }

    // get the name of the UserWorkspace given the user name
    protected static String getUserWorkspaceNameForUser(String userName) {
        return IdUtils.generateId(userName, "-", false, 30);
    }

    // compute the path of the Root of all userWorkspaces
    protected String computePathUserWorkspaceRoot(CoreSession userCoreSession,
            DocumentModel currentDocument) throws ClientException {
        String domainName = getDomainName(userCoreSession, currentDocument);
        if (domainName == null) {
            throw new ClientException(
                    "Unable to find root domain for UserWorkspace");
        }
        Path path = new Path("/" + domainName);
        path = path.append(UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        return path.toString();
    }

    // compute the path of the userWorkspace
    protected String computePathForUserWorkspace(CoreSession userCoreSession,
            String userName, DocumentModel currentDocument)
            throws ClientException {
        String rootPath = computePathUserWorkspaceRoot(userCoreSession,
                currentDocument);
        Path path = new Path(rootPath);
        path = path.append(getUserWorkspaceNameForUser(userName));
        return path.toString();
    }

    public DocumentModel getCurrentUserPersonalWorkspace(String userName,
            DocumentModel currentDocument) throws ClientException {
        if (currentDocument == null) {
            return null;
        }
        CoreSession userCoreSession = CoreInstance.getInstance().getSession(
                currentDocument.getSessionId());

        return getCurrentUserPersonalWorkspace(userName, userCoreSession,
                currentDocument);
    }

    public DocumentModel getCurrentUserPersonalWorkspace(
            CoreSession userCoreSession, DocumentModel context)
            throws ClientException {
        return getCurrentUserPersonalWorkspace(
                userCoreSession.getPrincipal().getName(), userCoreSession,
                context);
    }

    protected DocumentModel getCurrentUserPersonalWorkspace(String userName,
            CoreSession userCoreSession, DocumentModel context)
            throws ClientException {

        PathRef uwsDocRef = new PathRef(computePathForUserWorkspace(
                userCoreSession, userName, context));

        if (!userCoreSession.exists(uwsDocRef)) {
            // do the creation
            PathRef rootRef = new PathRef(computePathUserWorkspaceRoot(
                    userCoreSession, context));
            uwsDocRef = createUserWorkspace(rootRef, uwsDocRef,
                    userCoreSession, userName);
        }

        // force Session synchro to process invalidation (in non JCA cases)
        if (userCoreSession.getClass().getSimpleName().equals("LocalSession")) {
            userCoreSession.save();
        }

        return userCoreSession.getDocument(uwsDocRef);
    }

    protected synchronized PathRef createUserWorkspace(PathRef rootRef,
            PathRef userWSRef, CoreSession userCoreSession, String userName)
            throws ClientException {

        UnrestrictedUWSCreator creator = new UnrestrictedUWSCreator(rootRef,
                userWSRef, userCoreSession, userName);
        creator.runUnrestricted();
        userWSRef = creator.userWSRef;
        rootRef = creator.rootRef;
        return userWSRef;
    }

    protected class RootDomainFinder extends UnrestrictedSessionRunner {

        public RootDomainFinder(CoreSession userCoreSession) {
            super(userCoreSession);
        }

        protected String domaineName;

        @Override
        public void run() throws ClientException {
            PathRef ref = new PathRef("/"
                    + UserWorkspaceServiceImplComponent.getTargetDomainName());
            if (session.exists(ref)) {
                domaineName = UserWorkspaceServiceImplComponent.getTargetDomainName();
                return;
            }
            // configured domain does not exist !!!
            DocumentModelList domains = session.query("select * from Domain order by dc:created");

            if (!domains.isEmpty()) {
                domaineName = domains.get(0).getName();
            }
        }
    }

    protected class UnrestrictedUWSCreator extends UnrestrictedSessionRunner {

        PathRef rootRef;

        PathRef userWSRef;

        String userName;

        public UnrestrictedUWSCreator(PathRef rootRef, PathRef userWSRef,
                CoreSession userCoreSession, String userName) {
            super(userCoreSession);
            this.rootRef = rootRef;
            this.userWSRef = userWSRef;
            this.userName = userName;
        }

        @Override
        public void run() throws ClientException {

            // create root if needed
            if (!session.exists(rootRef)) {
                DocumentModel root = null;
                try {
                    root = createUserWorkspacesRoot(session, rootRef);
                } catch (Exception e) {
                    // domain may have been removed !
                    targetDomainName = null;
                    rootRef = new PathRef(computePathUserWorkspaceRoot(session,
                            null));
                    root = createUserWorkspacesRoot(session, rootRef);
                    userWSRef = new PathRef(computePathForUserWorkspace(
                            session, userName, null));
                }
                assert (root.getPathAsString().equals(rootRef.toString()));
            }

            // create user WS if needed
            if (!session.exists(userWSRef)) {
                DocumentModel uw = createUserWorkspace(session, userWSRef,
                        userName);
                assert (uw.getPathAsString().equals(userWSRef.toString()));
            }

            session.save();
        }

    }

    protected static DocumentModel createUserWorkspacesRoot(
            CoreSession unrestrictedSession, PathRef rootRef)
            throws ClientException {

        String parentPath = new Path(rootRef.toString()).removeLastSegments(1).toString();
        DocumentModel doc = unrestrictedSession.createDocumentModel(parentPath,
                UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT,
                "UserWorkspacesRoot");
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
                wsName, "Workspace");

        doc.setProperty("dublincore", "title",
                buildUserWorkspaceTitle(userName));
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

    public DocumentModel getUserPersonalWorkspace(String userName,
            DocumentModel context) throws ClientException {
        try {
            UnrestrictedUserWorkspaceFinder finder = new UnrestrictedUserWorkspaceFinder(
                    userName, context);
            finder.runUnrestricted();
            return finder.getDetachedUserWorkspace();
        } catch (Exception e) {
            log.warn("Error while trying to get user workspace unrestricted");
            throw new ClientException(e);
        }
    }

    public static String buildUserWorkspaceTitle(String userName) {
        if (userName == null) {// avoid looking for UserManager for nothing
            return null;
        }
        // get the user service
        UserManager userManager = null;
        try {
            userManager = Framework.getService(UserManager.class);
        } catch (Exception e) {
            log.debug("failed to get user service", e);
        }
        if (userManager == null) {
            return userName;
        }

        // get nuxeo pricipal
        NuxeoPrincipal principal = null;
        try {
            principal = userManager.getPrincipal(userName);
        } catch (ClientException e) {
            log.debug("failed to get principal" + userName, e);
        }

        if (principal == null) {
            return userName;
        }

        // compute the title
        StringBuilder title = new StringBuilder();
        String firstName = principal.getFirstName();
        if (firstName != null && firstName.trim().length() > 0) {
            title.append(firstName);
        }

        String lastName = principal.getLastName();
        if (lastName != null && lastName.trim().length() > 0) {
            if (title.length() > 0) {
                title.append(" ");
            }
            title.append(lastName);
        }

        if (title.length() > 0) {
            return title.toString();
        }

        return userName;

    }

    protected class UnrestrictedUserWorkspaceFinder extends
            UnrestrictedSessionRunner {

        protected DocumentModel userWorkspace;

        protected String userName;

        protected DocumentModel context;

        protected UnrestrictedUserWorkspaceFinder(String userName,
                DocumentModel context) throws Exception {
            super(context.getCoreSession().getRepositoryName(), userName);
            this.userName = userName;
            this.context = context;
        }

        @Override
        public void run() throws ClientException {
            userWorkspace = getCurrentUserPersonalWorkspace(userName, session,
                    context);
            if (userWorkspace != null) {
                userWorkspace.detach(true);
            }
        }

        public DocumentModel getDetachedUserWorkspace() throws ClientException {
            return userWorkspace;
        }
    }

}

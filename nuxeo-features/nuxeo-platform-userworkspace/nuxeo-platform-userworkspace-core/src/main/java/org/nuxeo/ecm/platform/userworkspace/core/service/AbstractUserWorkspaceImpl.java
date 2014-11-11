/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.usermanager.UserAdapter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class holding most of the logic for using
 * {@link UnrestrictedSessionRunner} while creating UserWorkspaces and
 * associated resources
 *
 * @author tiry
 * @since 5.9.5
 */
public abstract class AbstractUserWorkspaceImpl implements UserWorkspaceService {

    private static final Log log = LogFactory.getLog(DefaultUserWorkspaceServiceImpl.class);

    private static final long serialVersionUID = 1L;

    protected String targetDomainName;

    public AbstractUserWorkspaceImpl() {
        super();
    }

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

    protected String getUserWorkspaceNameForUser(String userName) {
        return IdUtils.generateId(userName, "-", false, 30);
    }

    protected String computePathUserWorkspaceRoot(CoreSession userCoreSession, String usedUsername,
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

    protected String computePathForUserWorkspace(CoreSession userCoreSession,
            String userName, DocumentModel currentDocument)
            throws ClientException {
        String rootPath = computePathUserWorkspaceRoot(userCoreSession, userName,
                currentDocument);
        Path path = new Path(rootPath);
        path = path.append(getUserWorkspaceNameForUser(userName));
        return path.toString();
    }

    @Override
    public DocumentModel getCurrentUserPersonalWorkspace(String userName,
            DocumentModel currentDocument) throws ClientException {
        if (currentDocument == null) {
            return null;
        }
        return getCurrentUserPersonalWorkspace(null, userName,
                currentDocument.getCoreSession(), currentDocument);
    }

    @Override
    public DocumentModel getCurrentUserPersonalWorkspace(
            CoreSession userCoreSession, DocumentModel context)
            throws ClientException {
        return getCurrentUserPersonalWorkspace(userCoreSession.getPrincipal(),
                null, userCoreSession, context);
    }

    /**
     * This method handles the UserWorkspace creation with a Principal or a
     * username. At least one should be passed. If a principal is passed, the
     * username is not taken into account.
     *
     * @since 5.7 "userWorkspaceCreated" is triggered
     */
    protected DocumentModel getCurrentUserPersonalWorkspace(
            Principal principal, String userName, CoreSession userCoreSession,
            DocumentModel context) throws ClientException {
        if (principal == null && StringUtils.isEmpty(userName)) {
            throw new ClientException(
                    "You should pass at least one principal or one username");
        }

        String usedUsername;
        if (principal instanceof NuxeoPrincipal) {
            usedUsername = ((NuxeoPrincipal) principal).getActingUser();
        } else {
            usedUsername = userName;
        }

        PathRef uwsDocRef = new PathRef(computePathForUserWorkspace(
                userCoreSession, usedUsername, context));

        if (!userCoreSession.exists(uwsDocRef)) {
            // do the creation
            PathRef rootRef = new PathRef(computePathUserWorkspaceRoot(
                    userCoreSession, usedUsername, context));
            uwsDocRef = createUserWorkspace(rootRef, uwsDocRef,
                    userCoreSession, principal, usedUsername);
        }

        // force Session synchro to process invalidation (in non JCA cases)
        if (userCoreSession.getClass().getSimpleName().equals("LocalSession")) {
            userCoreSession.save();
        }

        return userCoreSession.getDocument(uwsDocRef);
    }

    protected synchronized PathRef createUserWorkspace(PathRef rootRef,
            PathRef userWSRef, CoreSession userCoreSession,
            Principal principal, String userName) throws ClientException {

        UnrestrictedUWSCreator creator = new UnrestrictedUWSCreator(rootRef,
                userWSRef, userCoreSession, principal, userName);
        creator.runUnrestricted();
        userWSRef = creator.userWSRef;
        return userWSRef;
    }

    @Override
    public DocumentModel getUserPersonalWorkspace(NuxeoPrincipal principal,
            DocumentModel context) throws ClientException {
        return getCurrentUserPersonalWorkspace(principal, null,
                context.getCoreSession(), context);
    }

    @Override
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

    protected String buildUserWorkspaceTitle(Principal principal,
            String userName) {
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

        // Adapter userModel to get its fields (firstname, lastname)
        DocumentModel userModel = null;
        try {
            userModel = userManager.getUserModel(userName);
        } catch (ClientException e) {
            log.error("Unable to fetch user model", e);
        }

        if (userModel == null) {
            return userName;
        }

        UserAdapter userAdapter = null;
        userAdapter = userModel.getAdapter(UserAdapter.class);

        if (userAdapter == null) {
            return userName;
        }

        // compute the title
        StringBuilder title = new StringBuilder();
        try {
            String firstName = userAdapter.getFirstName();
            if (firstName != null && firstName.trim().length() > 0) {
                title.append(firstName);
            }

            String lastName = userAdapter.getLastName();
            if (lastName != null && lastName.trim().length() > 0) {
                if (title.length() > 0) {
                    title.append(" ");
                }
                title.append(lastName);
            }
        } catch (ClientException ce) {
            log.error("Failed to compute the title for " + userName
                    + "'s workspace", ce);
        }

        if (title.length() > 0) {
            return title.toString();
        }

        return userName;

    }

    protected void notifyEvent(CoreSession coreSession, DocumentModel document,
            NuxeoPrincipal principal, String eventId,
            Map<String, Serializable> properties) throws ClientException {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        EventContext eventContext = null;
        if (document != null) {
            properties.put(CoreEventConstants.REPOSITORY_NAME,
                    document.getRepositoryName());
            properties.put(CoreEventConstants.SESSION_ID,
                    coreSession.getSessionId());
            properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    document.getCurrentLifeCycleState());
            eventContext = new DocumentEventContext(coreSession, principal,
                    document);
        } else {
            eventContext = new EventContextImpl(coreSession, principal);
        }
        eventContext.setProperties(properties);
        Event event = eventContext.newEvent(eventId);
        Framework.getLocalService(EventProducer.class).fireEvent(event);
    }

    protected class UnrestrictedUWSCreator extends UnrestrictedSessionRunner {

        PathRef rootRef;

        PathRef userWSRef;

        String userName;

        Principal principal;

        public UnrestrictedUWSCreator(PathRef rootRef, PathRef userWSRef,
                CoreSession userCoreSession, Principal principal,
                String userName) {
            super(userCoreSession);
            this.rootRef = rootRef;
            this.userWSRef = userWSRef;
            this.userName = userName;
            this.principal = principal;
        }

        @Override
        public void run() throws ClientException {

            // create root if needed
            if (!session.exists(rootRef)) {
                DocumentModel root;
                try {
                    root = doCreateUserWorkspacesRoot(session, rootRef);
                } catch (Exception e) {
                    // domain may have been removed !
                    targetDomainName = null;
                    rootRef = new PathRef(computePathUserWorkspaceRoot(session,userName,
                            null));
                    root = doCreateUserWorkspacesRoot(session, rootRef);
                    userWSRef = new PathRef(computePathForUserWorkspace(
                            session, userName, null));
                }
                assert (root.getPathAsString().equals(rootRef.toString()));
            }

            // create user WS if needed
            if (!session.exists(userWSRef)) {
                DocumentModel uw = doCreateUserWorkspace(session, userWSRef,
                        principal, userName);
                assert (uw.getPathAsString().equals(userWSRef.toString()));
            }

            session.save();
        }

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
            userWorkspace = getCurrentUserPersonalWorkspace(null, userName,
                    session, context);
            if (userWorkspace != null) {
                userWorkspace.detach(true);
            }
        }

        public DocumentModel getDetachedUserWorkspace() throws ClientException {
            return userWorkspace;
        }
    }

    protected class RootDomainFinder extends UnrestrictedSessionRunner {

        public RootDomainFinder(CoreSession userCoreSession) {
            super(userCoreSession);
        }

        protected String domaineName;

        @Override
        public void run() throws ClientException {

            String targetName = getComponent().getTargetDomainName();
            PathRef ref = new PathRef("/" + targetName);
            if (session.exists(ref)) {
                domaineName = targetName;
                return;
            }
            // configured domain does not exist !!!
            DocumentModelList domains = session.query("select * from Domain order by dc:created");

            if (!domains.isEmpty()) {
                domaineName = domains.get(0).getName();
            }
        }
    }

    protected UserWorkspaceServiceImplComponent getComponent() {
        return (UserWorkspaceServiceImplComponent) Framework.getRuntime().getComponent(
                UserWorkspaceServiceImplComponent.NAME);
    }

    protected abstract DocumentModel doCreateUserWorkspacesRoot(
            CoreSession unrestrictedSession, PathRef rootRef)
            throws ClientException;

    protected abstract DocumentModel doCreateUserWorkspace(
            CoreSession unrestrictedSession, PathRef wsRef,
            Principal principal, String userName) throws ClientException;

}
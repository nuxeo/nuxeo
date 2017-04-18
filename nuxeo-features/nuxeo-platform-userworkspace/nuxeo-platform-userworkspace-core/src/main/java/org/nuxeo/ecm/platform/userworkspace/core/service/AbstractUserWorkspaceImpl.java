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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
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
 * Abstract class holding most of the logic for using {@link UnrestrictedSessionRunner} while creating UserWorkspaces
 * and associated resources
 *
 * @author tiry
 * @since 5.9.5
 */
public abstract class AbstractUserWorkspaceImpl implements UserWorkspaceService {

    private static final Log log = LogFactory.getLog(DefaultUserWorkspaceServiceImpl.class);

    private static final long serialVersionUID = 1L;

    protected static final char ESCAPE_CHAR = '~';

    protected static final String ESCAPED_CHARS = ESCAPE_CHAR + "/\\?&;@";

    protected String targetDomainName;

    protected final int maxsize;

    public AbstractUserWorkspaceImpl() {
        super();
        maxsize = Framework.getService(PathSegmentService.class)
                .getMaxSize();
    }

    protected String getDomainName(CoreSession userCoreSession, DocumentModel currentDocument) {
        if (targetDomainName == null) {
            RootDomainFinder finder = new RootDomainFinder(userCoreSession);
            finder.runUnrestricted();
            targetDomainName = finder.domaineName;
        }
        return targetDomainName;
    }

    /**
     * Gets the base username to use to determine a user's workspace. This is not used directly as a path segment, but
     * forms the sole basis for it.
     *
     * @since 9.2
     */
    protected String getUserName(Principal principal, String username) {
        if (principal instanceof NuxeoPrincipal) {
            username = ((NuxeoPrincipal) principal).getActingUser();
        } else if (username == null) {
            username = principal.getName();
        }
        if (StringUtils.isEmpty(username)) {
            username = null;
        }
        return username;
    }

    /**
     * Finds the list of potential names for the user workspace. They're all tried in order.
     *
     * @return the list of candidate names
     * @since 9.2
     */
    // public for tests
    public List<String> getCandidateUserWorkspaceNames(String username) {
        List<String> names = new ArrayList<>();
        names.add(escape(username));
        generateCandidates(names, username, maxsize); // compat
        generateCandidates(names, username, 30); // old compat
        return names;
    }

    /**
     * Bijective escaping for user names.
     * <p>
     * Escapes some chars not allowed in a path segment or URL. The escaping character is a {@code ~} followed by the
     * one-byte hex value of the character.
     *
     * @since 9.2
     */
    protected String escape(String string) {
        StringBuilder buf = new StringBuilder(string.length());
        for (char c : string.toCharArray()) {
            if (ESCAPED_CHARS.indexOf(c) == -1) {
                buf.append(c);
            } else {
                buf.append(ESCAPE_CHAR);
                if (c < 16) {
                    buf.append('0');
                }
                buf.append(Integer.toHexString(c)); // assumed to be < 256
            }
        }
        // don't re-allocate a new string if we didn't escape anything
        return buf.length() == string.length() ? string : buf.toString();
    }

    protected void generateCandidates(List<String> names, String username, int max) {
        String name = IdUtils.generateId(username, "-", false, max);
        if (!names.contains(name)) {
            names.add(name);
        }
        if (name.length() == max) { // at max size or truncated
            String digested = name.substring(0, name.length() - 8) + digest(username, 8);
            if (!names.contains(digested)) {
                names.add(digested);
            }
        }
    }

    protected String digest(String username, int maxsize) {
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.update(username.getBytes());
            return new String(Hex.encodeHex(crypt.digest())).substring(0, maxsize);
        } catch (NoSuchAlgorithmException cause) {
            throw new NuxeoException("Cannot digest " + username, cause);
        }
    }

    protected String computePathUserWorkspaceRoot(CoreSession userCoreSession, String usedUsername,
            DocumentModel currentDocument) {
        String domainName = getDomainName(userCoreSession, currentDocument);
        if (domainName == null) {
            throw new NuxeoException("Unable to find root domain for UserWorkspace");
        }
        return new Path("/" + domainName)
                .append(UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT)
                .toString();
    }

    @Override
    public DocumentModel getCurrentUserPersonalWorkspace(String userName, DocumentModel currentDocument)
            {
        if (currentDocument == null) {
            return null;
        }
        return getCurrentUserPersonalWorkspace(null, userName, currentDocument.getCoreSession(), currentDocument);
    }

    @Override
    public DocumentModel getCurrentUserPersonalWorkspace(CoreSession userCoreSession, DocumentModel context)
            {
        return getCurrentUserPersonalWorkspace(userCoreSession.getPrincipal(), null, userCoreSession, context);
    }

    /**
     * This method handles the UserWorkspace creation with a Principal or a username. At least one should be passed. If
     * a principal is passed, the username is not taken into account.
     *
     * @since 5.7 "userWorkspaceCreated" is triggered
     */
    protected DocumentModel getCurrentUserPersonalWorkspace(Principal principal, String userName,
            CoreSession userCoreSession, DocumentModel context) {
        String usedUsername = getUserName(principal, userName);
        if (usedUsername == null) {
            return null;
        }
        PathRef rootref = getExistingUserWorkspaceRoot(userCoreSession, usedUsername, context);
        PathRef uwref = getExistingUserWorkspace(userCoreSession, rootref, principal, usedUsername);
        DocumentModel uw = userCoreSession.getDocument(uwref);

        return uw;
    }

    protected PathRef getExistingUserWorkspaceRoot(CoreSession session, String username, DocumentModel context) {
        PathRef rootref = new PathRef(computePathUserWorkspaceRoot(session, username, context));
        if (session.exists(rootref)) {
            return rootref;
        }
        return new PathRef(new UnrestrictedRootCreator(rootref, username, session).create().getPathAsString());
    }

    protected PathRef getExistingUserWorkspace(CoreSession session, PathRef rootref, Principal principal,
            String username) {
        PathRef freeRef = null;
        for (String name : getCandidateUserWorkspaceNames(username)) {
            PathRef ref = new PathRef(rootref, name);
            if (session.exists(ref)
                    && session.hasPermission(session.getPrincipal(), ref, SecurityConstants.EVERYTHING)) {
                return ref;
            }
            boolean[] exists = new boolean[1];
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() {
                    exists[0] = session.exists(ref);
                }
            }.runUnrestricted();
            if (!exists[0] && freeRef == null) {
                // we have a candidate name for creation if we don't find anything else
                freeRef = ref;
            }
            // else if exists it means there's a collision with the truncated workspace of another user
            // try next name
        }
        if (freeRef != null) {
            PathRef ref = freeRef; // effectively final
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() {
                    doCreateUserWorkspace(session, ref, principal, username);
                }
            }.runUnrestricted();
            return freeRef;
        }
        // couldn't find anything, because we lacked permission to existing docs (collision)
        throw new DocumentSecurityException(username);
    }

    @Override
    public DocumentModel getUserPersonalWorkspace(NuxeoPrincipal principal, DocumentModel context)
            {
        return getCurrentUserPersonalWorkspace(principal, null, context.getCoreSession(), context);
    }

    @Override
    public DocumentModel getUserPersonalWorkspace(String userName, DocumentModel context) {
        UnrestrictedUserWorkspaceFinder finder = new UnrestrictedUserWorkspaceFinder(userName, context);
        finder.runUnrestricted();
        return finder.getDetachedUserWorkspace();
    }

    @Override
    public boolean isUnderUserWorkspace(Principal principal, String username, DocumentModel doc) {
        if (doc == null) {
            return false;
        }
        username = getUserName(principal, username);
        if (username == null) {
            return false;
        }

        // fast checks that are useful to return a negative without the cost of accessing the user workspace
        Path path = doc.getPath();
        if (path.segmentCount() < 2) {
            return false;
        }
        // check domain
        String domainName = getDomainName(doc.getCoreSession(), doc);
        if (!domainName.equals(path.segment(0))) {
            return false;
        }
        // check UWS root
        if (!UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT.equals(path.segment(1))) {
            return false;
        }
        // check workspace name among candidates
        if (!getCandidateUserWorkspaceNames(username).contains(path.segment(2))) {
            return false;
        }

        // fetch actual workspace to compare its path
        DocumentModel uws = getCurrentUserPersonalWorkspace(principal, username, doc.getCoreSession(), doc);
        return uws.getPath().isPrefixOf(doc.getPath());
    }

    protected String buildUserWorkspaceTitle(Principal principal, String userName) {
        if (userName == null) {// avoid looking for UserManager for nothing
            return null;
        }
        // get the user service
        UserManager userManager = Framework.getService(UserManager.class);
        if (userManager == null) {
            // for tests
            return userName;
        }

        // Adapter userModel to get its fields (firstname, lastname)
        DocumentModel userModel = userManager.getUserModel(userName);
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
        String firstName = userAdapter.getFirstName();
        if (firstName != null && firstName.trim()
                .length() > 0) {
            title.append(firstName);
        }

        String lastName = userAdapter.getLastName();
        if (lastName != null && lastName.trim()
                .length() > 0) {
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

    protected void notifyEvent(CoreSession coreSession, DocumentModel document, NuxeoPrincipal principal,
            String eventId, Map<String, Serializable> properties) {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        EventContext eventContext = null;
        if (document != null) {
            properties.put(CoreEventConstants.REPOSITORY_NAME, document.getRepositoryName());
            properties.put(CoreEventConstants.SESSION_ID, coreSession.getSessionId());
            properties.put(CoreEventConstants.DOC_LIFE_CYCLE, document.getCurrentLifeCycleState());
            eventContext = new DocumentEventContext(coreSession, principal, document);
        } else {
            eventContext = new EventContextImpl(coreSession, principal);
        }
        eventContext.setProperties(properties);
        Event event = eventContext.newEvent(eventId);
        Framework.getLocalService(EventProducer.class)
                .fireEvent(event);
    }

    protected class UnrestrictedRootCreator extends UnrestrictedSessionRunner {
        public UnrestrictedRootCreator(PathRef ref, String username, CoreSession session) {
            super(session);
            this.ref = ref;
            this.username = username;
        }
        PathRef ref;
        final String username;
        DocumentModel doc;

        @Override
        public void run() {
            if (session.exists(ref)) {
                doc = session.getDocument(ref);
            } else {
                try {
                    doc = doCreateUserWorkspacesRoot(session, ref);
                } catch (DocumentNotFoundException e) {
                    // domain may have been removed !
                    targetDomainName = null;
                    ref = new PathRef(computePathUserWorkspaceRoot(session, username, null));
                    doc = doCreateUserWorkspacesRoot(session, ref);
                }
            }
            doc.detach(true);
            assert (doc.getPathAsString()
                    .equals(ref.toString()));
        }

        DocumentModel create() {
            synchronized (UnrestrictedRootCreator.class) {
                runUnrestricted();
                return doc;
            }
        }
    }

    protected class UnrestrictedUserWorkspaceFinder extends UnrestrictedSessionRunner {

        protected DocumentModel userWorkspace;

        protected String userName;

        protected DocumentModel context;

        protected UnrestrictedUserWorkspaceFinder(String userName, DocumentModel context) {
            super(context.getCoreSession()
                    .getRepositoryName(), userName);
            this.userName = userName;
            this.context = context;
        }

        @Override
        public void run() {
            userWorkspace = getCurrentUserPersonalWorkspace(null, userName, session, context);
            if (userWorkspace != null) {
                userWorkspace.detach(true);
            }
        }

        public DocumentModel getDetachedUserWorkspace() {
            return userWorkspace;
        }
    }

    protected class RootDomainFinder extends UnrestrictedSessionRunner {

        public RootDomainFinder(CoreSession userCoreSession) {
            super(userCoreSession);
        }

        protected String domaineName;

        @Override
        public void run() {

            String targetName = getComponent().getTargetDomainName();
            PathRef ref = new PathRef("/" + targetName);
            if (session.exists(ref)) {
                domaineName = targetName;
                return;
            }
            // configured domain does not exist !!!
            DocumentModelList domains = session.query("select * from Domain order by dc:created");

            if (!domains.isEmpty()) {
                domaineName = domains.get(0)
                        .getName();
            }
        }
    }

    protected UserWorkspaceServiceImplComponent getComponent() {
        return (UserWorkspaceServiceImplComponent) Framework.getRuntime()
                .getComponent(
                        UserWorkspaceServiceImplComponent.NAME);
    }

    protected abstract DocumentModel doCreateUserWorkspacesRoot(CoreSession unrestrictedSession, PathRef rootRef);

    protected abstract DocumentModel doCreateUserWorkspace(CoreSession unrestrictedSession, PathRef wsRef,
            Principal principal, String userName);

}

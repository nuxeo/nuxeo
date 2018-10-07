/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionLocationService;
import org.nuxeo.ecm.collections.api.FavoritesConstants;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.usermanager.UserAdapter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class holding most of the logic for using {@link UnrestrictedSessionRunner} while creating UserWorkspaces
 * and associated resources
 *
 * @author tiry
 * @since 5.9.5
 */
public abstract class AbstractUserWorkspaceImpl implements UserWorkspaceService, CollectionLocationService {

    private static final long serialVersionUID = 1L;

    protected static final char ESCAPE_CHAR = '~';

    protected static final String ESCAPED_CHARS = ESCAPE_CHAR + "/\\?&;@";

    protected volatile String targetDomainName;

    protected final int maxsize;

    public AbstractUserWorkspaceImpl() {
        super();
        maxsize = Framework.getService(PathSegmentService.class).getMaxSize();
    }

    protected String getDomainName(CoreSession userCoreSession) {
        if (targetDomainName == null) {
            CoreInstance.doPrivileged(userCoreSession, (CoreSession session) -> {
                String targetName = getComponent().getTargetDomainName();
                PathRef ref = new PathRef("/" + targetName);
                if (session.exists(ref)) {
                    targetDomainName = targetName;
                    return;
                }
                // configured domain does not exist !!!
                DocumentModelList domains = session.query(
                        "select * from Domain where ecm:isTrashed = 0 order by dc:created");

                if (!domains.isEmpty()) {
                    targetDomainName = domains.get(0).getName();
                }
            });
        }
        return targetDomainName;
    }

    /**
     * Gets the base username to use to determine a user's workspace. This is not used directly as a path segment, but
     * forms the sole basis for it.
     *
     * @since 9.2
     */
    protected String getUserName(NuxeoPrincipal principal, String username) {
        if (principal != null) {
            username = principal.getActingUser();
        }
        if (NuxeoPrincipal.isTransientUsername(username)) {
            // no personal workspace for transient users
            username = null;
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

    protected String computePathUserWorkspaceRoot(CoreSession userCoreSession, String usedUsername) {
        String domainName = getDomainName(userCoreSession);
        if (domainName == null) {
            return null;
        }
        return new Path("/" + domainName).append(UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT).toString();
    }

    @Override
    public DocumentModel getCurrentUserPersonalWorkspace(String userName, DocumentModel currentDocument) {
        return getCurrentUserPersonalWorkspace(null, userName, currentDocument.getCoreSession());
    }

    @Override
    public DocumentModel getCurrentUserPersonalWorkspace(CoreSession userCoreSession) {
        return getCurrentUserPersonalWorkspace(userCoreSession.getPrincipal(), null, userCoreSession);
    }

    /**
     * Only for compatibility.
     *
     * @deprecated since 9.3
     */
    @Override
    public DocumentModel getCurrentUserPersonalWorkspace(CoreSession userCoreSession, DocumentModel context) {
        return getCurrentUserPersonalWorkspace(userCoreSession);
    }

    /**
     * This method handles the UserWorkspace creation with a Principal or a username. At least one should be passed. If
     * a principal is passed, the username is not taken into account.
     *
     * @since 5.7 "userWorkspaceCreated" is triggered
     */
    protected DocumentModel getCurrentUserPersonalWorkspace(NuxeoPrincipal principal, String userName,
            CoreSession userCoreSession) {
        String usedUsername = getUserName(principal, userName);
        if (usedUsername == null) {
            return null;
        }
        PathRef rootref = getExistingUserWorkspaceRoot(userCoreSession, usedUsername);
        if (rootref == null) {
            return null;
        }
        PathRef uwref = getExistingUserWorkspace(userCoreSession, rootref, principal, usedUsername);
        DocumentModel uw = userCoreSession.getDocument(uwref);

        return uw;
    }

    protected PathRef getExistingUserWorkspaceRoot(CoreSession session, String username) {
        String uwrPath = computePathUserWorkspaceRoot(session, username);
        if (uwrPath == null) {
            return null;
        }
        PathRef rootref = new PathRef(uwrPath);
        if (session.exists(rootref)) {
            return rootref;
        }

        String path = CoreInstance.doPrivileged(session, s -> {
            DocumentModel uwsRootModel = doCreateUserWorkspacesRoot(session, rootref);
            DocumentModel docModel = s.getOrCreateDocument(uwsRootModel, doc -> initCreateUserWorkspacesRoot(s, doc));
            return docModel.getPathAsString();
        });

        return new PathRef(path);
    }

    protected PathRef getExistingUserWorkspace(CoreSession session, PathRef rootref, NuxeoPrincipal principal,
            String username) {
        PathRef freeRef = null;
        for (String name : getCandidateUserWorkspaceNames(username)) {
            PathRef ref = new PathRef(rootref, name);
            if (session.exists(ref)
                    && session.hasPermission(session.getPrincipal(), ref, SecurityConstants.EVERYTHING)) {
                return ref;
            }
            @SuppressWarnings("boxing")
            boolean exists = CoreInstance.doPrivileged(session, (CoreSession s) -> s.exists(ref));
            if (!exists && freeRef == null) {
                // we have a candidate name for creation if we don't find anything else
                freeRef = ref;
            }
            // else if exists it means there's a collision with the truncated workspace of another user
            // try next name
        }
        if (freeRef != null) {
            PathRef ref = freeRef; // effectively final
            String path = CoreInstance.doPrivileged(session, s -> {
                DocumentModel uwsModel = doCreateUserWorkspace(session, ref, username);
                return s.getOrCreateDocument(uwsModel, doc -> initCreateUserWorkspace(s, doc, username))
                        .getPathAsString();
            });
            return new PathRef(path);
        }
        // couldn't find anything, because we lacked permission to existing docs (collision)
        throw new DocumentSecurityException(username);
    }

    @Override
    public DocumentModel getUserPersonalWorkspace(NuxeoPrincipal principal, DocumentModel context) {
        return getCurrentUserPersonalWorkspace(principal, null, context.getCoreSession());
    }

    @Override
    public DocumentModel getUserPersonalWorkspace(String userName, DocumentModel context) {
        UnrestrictedUserWorkspaceFinder finder = new UnrestrictedUserWorkspaceFinder(userName, context);
        finder.runUnrestricted();
        return finder.getDetachedUserWorkspace();
    }

    @Override
    public boolean isUnderUserWorkspace(NuxeoPrincipal principal, String username, DocumentModel doc) {
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
        String domainName = getDomainName(doc.getCoreSession());
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
        DocumentModel uws = getCurrentUserPersonalWorkspace(principal, username, doc.getCoreSession());
        return uws.getPath().isPrefixOf(doc.getPath());
    }

    protected String buildUserWorkspaceTitle(String userName) {
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

        UserAdapter userAdapter;
        userAdapter = userModel.getAdapter(UserAdapter.class);

        if (userAdapter == null) {
            return userName;
        }

        // compute the title
        StringBuilder title = new StringBuilder();
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

        if (title.length() > 0) {
            return title.toString();
        }

        return userName;

    }

    protected void notifyEvent(CoreSession coreSession, DocumentModel document, NuxeoPrincipal principal,
            String eventId, Map<String, Serializable> properties) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        EventContext eventContext;
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
        Framework.getService(EventProducer.class).fireEvent(event);
    }

    protected class UnrestrictedUserWorkspaceFinder extends UnrestrictedSessionRunner {

        protected DocumentModel userWorkspace;

        protected String userName;

        protected UnrestrictedUserWorkspaceFinder(String userName, DocumentModel context) {
            super(context.getCoreSession().getRepositoryName(), userName);
            this.userName = userName;
        }

        @Override
        public void run() {
            userWorkspace = getCurrentUserPersonalWorkspace(null, userName, session);
            if (userWorkspace != null) {
                userWorkspace.detach(true);
            }
        }

        public DocumentModel getDetachedUserWorkspace() {
            return userWorkspace;
        }
    }

    protected UserWorkspaceServiceImplComponent getComponent() {
        return (UserWorkspaceServiceImplComponent) Framework.getRuntime()
                                                            .getComponent(UserWorkspaceServiceImplComponent.NAME);
    }

    protected abstract DocumentModel doCreateUserWorkspacesRoot(CoreSession unrestrictedSession, PathRef rootRef);

    protected abstract DocumentModel initCreateUserWorkspacesRoot(CoreSession unrestrictedSession, DocumentModel doc);

    protected abstract DocumentModel doCreateUserWorkspace(CoreSession unrestrictedSession, PathRef wsRef,
            String username);

    protected abstract DocumentModel initCreateUserWorkspace(CoreSession unrestrictedSession, DocumentModel doc,
            String username);

    @Override
    public void invalidate() {
        targetDomainName = null;
    }

    /**
     * @since 10.3
     */
    @Override
    public DocumentModel getUserDefaultCollectionsRoot(CoreSession session) {
        DocumentModel defaultCollectionsRoot = createDefaultCollectionsRoot(session,
                getCurrentUserPersonalWorkspace(session));
        return session.getOrCreateDocument(defaultCollectionsRoot, doc -> initDefaultCollectionsRoot(session, doc));
    }

    /**
     * @since 10.3
     */
    @Override
    public DocumentModel getUserFavorites(CoreSession session) {
        DocumentModel location = getCurrentUserPersonalWorkspace(session);
        if (location == null) {
            // no location => no favorites (transient user for instance)
            return null;
        }
        DocumentModel favorites = createFavorites(session, location);
        return session.getOrCreateDocument(favorites, doc -> initCreateFavorites(session, doc));
    }

    /**
     * @since 10.3
     */
    protected Locale getLocale(final CoreSession session) {
        Locale locale = null;
        locale = Framework.getService(LocaleProvider.class).getLocale(session);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return new Locale(Locale.getDefault().getLanguage());
    }

    /**
     * @since 10.3
     */
    protected DocumentModel createFavorites(CoreSession session, DocumentModel userWorkspace) {
        DocumentModel doc = session.createDocumentModel(userWorkspace.getPath().toString(),
                FavoritesConstants.DEFAULT_FAVORITES_NAME, FavoritesConstants.FAVORITES_TYPE);
        String title = null;
        try {
            title = I18NUtils.getMessageString("messages", FavoritesConstants.DEFAULT_FAVORITES_TITLE, new Object[0],
                    getLocale(session));
        } catch (MissingResourceException e) {
            title = FavoritesConstants.DEFAULT_FAVORITES_NAME;
        }
        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("dc:description", "");
        return doc;
    }

    /**
     * @since 10.3
     */
    protected DocumentModel initCreateFavorites(CoreSession session, DocumentModel favorites) {
        ACP acp = new ACPImpl();
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        ACE allowEverything = new ACE(session.getPrincipal().getName(), SecurityConstants.EVERYTHING, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowEverything, denyEverything });
        acp.addACL(acl);
        favorites.setACP(acp, true);
        return favorites;
    }

    /**
     * @since 10.3
     */
    protected DocumentModel createDefaultCollectionsRoot(final CoreSession session, DocumentModel userWorkspace) {
        DocumentModel doc = session.createDocumentModel(userWorkspace.getPath().toString(),
                CollectionConstants.DEFAULT_COLLECTIONS_NAME, CollectionConstants.COLLECTIONS_TYPE);
        String title;
        try {
            title = I18NUtils.getMessageString("messages", CollectionConstants.DEFAULT_COLLECTIONS_TITLE, new Object[0],
                    getLocale(session));
        } catch (MissingResourceException e) {
            title = CollectionConstants.DEFAULT_COLLECTIONS_TITLE;
        }
        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("dc:description", "");
        return doc;
    }

    /**
     * @since 10.3
     */
    protected DocumentModel initDefaultCollectionsRoot(final CoreSession session, DocumentModel collectionsRoot) {
        ACP acp = new ACPImpl();
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        ACE allowEverything = new ACE(session.getPrincipal().getName(), SecurityConstants.EVERYTHING, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowEverything, denyEverything });
        acp.addACL(acl);
        collectionsRoot.setACP(acp, true);
        return collectionsRoot;
    }

}

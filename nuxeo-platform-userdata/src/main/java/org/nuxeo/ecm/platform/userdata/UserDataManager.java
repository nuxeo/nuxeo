/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.userdata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class UserDataManager {

    private static final Log log = LogFactory.getLog(UserDataManager.class);

    /**
     * Name of User data container for the whole domain
     */
    public static final String USER_DATA_ROOT = "UserDatas";

    /**
     * @deprecated Use add(domainPath, session, username, category, docModel)
     * instead. See NXP-1617
     *
     */
    @Deprecated
    public void add(DocumentModel domain, String username,
            String category, DocumentModel docModel) throws ClientException {
        String domainPath = domain.getPathAsString();
        CoreSession session = CoreInstance.getInstance().getSession(domain.getSessionId());
        add(domainPath, session, username, category, docModel);
    }

    /**
     * Create a new document in the userdata area from a document model.
     * <p>
     * The passed document model is actually copied,
     *    since it's assumed to be transient
     * <p>
     * The user's data area as well as the whole <code>UserDatas</code>
     * root for this domain are created by a system session, if needed.
     *
     * @param domainPath The path to target domain
     * @param session User Core Session
     * @param username Name of user
     * @param category Category within user's data (e.g: <code>"searches"</code>)
     * @param docModel Document Model to copy
     * @throws ClientException
     */
    public void add(String domainPath, CoreSession session,
            String username, String category, DocumentModel docModel)
        throws ClientException {

        PathRef parentRef = getOrCreateCategoryFolder(
                username, category, session, new PathRef(domainPath));

        // GR TODO add unicity logic ?
        // GR TODO find out if the core has a logic for this
        // and add it if needed (session.attachDocument(parentPath))
        DocumentModel newDocModel = session.createDocumentModel(
                parentRef.toString(), docModel.getName(),
                docModel.getType());
        newDocModel.copyContent(docModel);
        session.createDocument(newDocModel);
        session.save();
    }

    /**
     * Guarantees existence and permissions of this user's data folder.
     *
     * @param username the user
     * @param session an existing session for this user
     * @param domainRef A {@link PathRef} to the current domain
     * @return A {@link PathRef} to the user data folder.
     * @throws ClientException
     */
    private PathRef getOrCreateUserDataFolder(String username,
            CoreSession session, PathRef domainRef)
            throws ClientException {

        PathRef rootRef = new PathRef(domainRef.toString(),
                USER_DATA_ROOT);
        PathRef userDataRef = new PathRef(rootRef.toString(), username);
        try {
            if (session.exists(userDataRef)) {
                return userDataRef;
            }
        }
        catch (ClientException e) {
            // Missing in some odd way or insufficient rights.
            // The code below will handle non pathological cases.
        }

        CoreSession systemSession = getSystemSession(session);

        // create root if needed
        if (!systemSession.exists(rootRef)) {
            log.info("Creating root of user personal data folders: "
                    + rootRef.toString());
            DocumentModel dm = systemSession.createDocumentModel(
                    domainRef.toString(), USER_DATA_ROOT, "HiddenFolder");
            dm.setProperty("dublincore", "title", username);
            dm = systemSession.createDocument(dm);
        }

        // create user data folder if needed
        DocumentModel userDataFolder;
        if (systemSession.exists(userDataRef)) {
            userDataFolder = systemSession.getDocument(userDataRef);
        } else {
            log.info("creating " + userDataRef.toString());
            DocumentModel dm = systemSession.createDocumentModel(
                    rootRef.toString(), username, "HiddenFolder");
            // TODO maybe put user full name as title ?
            dm.setProperty("dublincore", "title", username);
            userDataFolder = systemSession.createDocument(dm);
            systemSession.save(); // need to access with user session
        }

        // handle permissions on user data folder
        if (!session.hasPermission(userDataRef,
                SecurityConstants.READ_WRITE)) {
            ACP acp = userDataFolder.getACP();
            if (null == acp) {
                acp = new ACPImpl();
            }
            UserEntryImpl userEntry = new UserEntryImpl(username);
            userEntry.addPrivilege(SecurityConstants.READ_WRITE, true, false);
            UserEntry[] userEntries = new UserEntry[1];
            userEntries[0] = userEntry;
            acp.setRules(userEntries);
            systemSession.setACP(userDataRef, acp, true);
            systemSession.save();
        }

        CoreInstance.getInstance().close(systemSession);
        return userDataRef;
    }

    private PathRef getOrCreateCategoryFolder(String username,
            String category, CoreSession session, PathRef domainRef)
            throws ClientException {

        PathRef userDataRef = getOrCreateUserDataFolder(username,
                session, domainRef);

        // create category subfolder if needed
        PathRef pathRef = new PathRef(userDataRef.toString(), category);
        if (!session.exists(pathRef)) {
            log.info("Creating category subfolder: " + pathRef.toString());
            DocumentModel dm = session.createDocumentModel(
                    userDataRef.toString(), category, "HiddenFolder");
            dm.setProperty("dublincore", "title", category);
            dm = session.createDocument(dm);
            session.save();
        }

        return pathRef;
    }

    private CoreSession getSystemSession(CoreSession userSession)
            throws ClientException {
        CoreSession systemSession;
        try {
            Framework.login();
            RepositoryManager manager = Framework
                    .getService(RepositoryManager.class);
            systemSession = manager.getRepository(
                    userSession.getRepositoryName()).open();
        } catch (Exception e) {
            throw new ClientException("Failed to acquire system privileges", e);
        }
        return systemSession;
    }

    /**
     * @deprecated Use <code>remove(domainPath, session, username,
     * category, docModel) instead. See NXP-1617
     *
     */
    @Deprecated
    public static void remove(DocumentModel domain, String username, String category,
            DocumentModel docModel) throws ClientException {
        String domainPath = domain.getPathAsString();
        CoreSession session = CoreInstance.getInstance().getSession(domain.getSessionId());
        remove(domainPath, session, username, category, docModel);
    }

    public static void remove(String domainPath, CoreSession session, String username, String category,
            DocumentModel docModel) throws ClientException {
        session.removeDocument(docModel.getRef());
    }

    /**
     * @deprecated Use <code>get(domainPath, session, username,
     *             category) instead. See NXP-1617
     *
     */
    @Deprecated
    public static DocumentModelList get(DocumentModel domain, String username,
            String category) throws ClientException {
        String domainPath = domain.getPathAsString();
        CoreSession session = CoreInstance.getInstance().getSession(domain.getSessionId());
        return get(domainPath, session, username, category);
    }

    public static DocumentModelList get(String domainPath, CoreSession session,
            String username, String category) throws ClientException {
        if (domainPath == null) {
            throw new IllegalArgumentException("domainPath cannot be null");
        }

        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }

        if (category == null) {
            throw new IllegalArgumentException("category cannot be null");
        }

        PathRef ref = new PathRef(domainPath);
        String[] elements = new String[] {USER_DATA_ROOT,
                username, category};
        for (String elt: elements) {
            ref = new PathRef(ref.toString(), elt);
        }

        if (!session.exists(ref)) {
            return new DocumentModelListImpl();
        }
        return session.getChildren(ref);
    }

}

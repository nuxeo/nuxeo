/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_DOCTYPE;

import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Implementation of {@code UserProfileService}.
 *
 * @see UserProfileService
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.5
 */
public class UserProfileServiceImpl extends DefaultComponent implements UserProfileService, ComponentManager.Listener {

    protected static final Integer CACHE_CONCURRENCY_LEVEL = 10;

    protected static final Integer CACHE_TIMEOUT = 10;

    protected static final Integer CACHE_MAXIMUM_SIZE = 1000;

    public static final String CONFIG_EP = "config";

    protected final Cache<String, String> profileUidCache = CacheBuilder.newBuilder()
                                                                        .concurrencyLevel(CACHE_CONCURRENCY_LEVEL)
                                                                        .maximumSize(CACHE_MAXIMUM_SIZE)
                                                                        .expireAfterWrite(CACHE_TIMEOUT,
                                                                                TimeUnit.MINUTES)
                                                                        .build();

    @Override
    public DocumentModel getUserProfileDocument(CoreSession session) {
        DocumentModel userWorkspace = Framework.getService(UserWorkspaceService.class)
                                               .getCurrentUserPersonalWorkspace(session, null);
        if (userWorkspace == null) {
            return null;
        }

        String uid = profileUidCache.getIfPresent(session.getPrincipal().getName());
        final IdRef ref = new IdRef(uid);
        if (uid != null && session.exists(ref)) {
            return session.getDocument(ref);
        } else {
            DocumentModel profile = new UserProfileDocumentGetter(session, userWorkspace).getOrCreate();
            profileUidCache.put(session.getPrincipal().getName(), profile.getId());
            return profile;
        }
    }

    @Override
    public DocumentModel getUserProfileDocument(String userName, CoreSession session) {
        DocumentModel userWorkspace = Framework.getService(UserWorkspaceService.class)
                                               .getUserPersonalWorkspace(userName, session.getRootDocument());
        if (userWorkspace == null) {
            return null;
        }

        String uid = profileUidCache.getIfPresent(userName);
        final IdRef ref = new IdRef(uid);
        if (uid != null && session.exists(ref)) {
            return session.getDocument(ref);
        } else {
            DocumentModel profile = new UserProfileDocumentGetter(session, userWorkspace).getOrCreate();
            profileUidCache.put(userName, profile.getId());
            return profile;
        }
    }

    @Override
    public DocumentModel getUserProfile(DocumentModel userModel, CoreSession session) {
        DocumentModel userProfileDoc = getUserProfileDocument(userModel.getId(), session);
        if (userProfileDoc == null) {
            return null;
        }

        userProfileDoc.detach(true);
        userProfileDoc.getDataModels().putAll(userModel.getDataModels());
        return userProfileDoc;
    }

    private class UserProfileDocumentGetter extends UnrestrictedSessionRunner {

        private DocumentModel userWorkspace;

        private DocumentRef userProfileDocRef;

        public UserProfileDocumentGetter(CoreSession session, DocumentModel userWorkspace) {
            super(session);
            this.userWorkspace = userWorkspace;
        }

        @Override
        public void run() {

            String query = "select * from " + USER_PROFILE_DOCTYPE + " where ecm:parentId='" + userWorkspace.getId()
                    + "' " + " AND ecm:isProxy = 0 " + " AND ecm:isVersion = 0 AND ecm:isTrashed = 0";
            DocumentModelList children = session.query(query);
            if (!children.isEmpty()) {
                userProfileDocRef = children.get(0).getRef();
            } else {
                DocumentModel userProfileDoc = session.createDocumentModel(userWorkspace.getPathAsString(),
                        String.valueOf(System.currentTimeMillis()), USER_PROFILE_DOCTYPE);
                userProfileDoc = session.createDocument(userProfileDoc);
                userProfileDocRef = userProfileDoc.getRef();
                ACP acp = session.getACP(userProfileDocRef);
                ACL acl = acp.getOrCreateACL();
                acl.add(new ACE(EVERYONE, READ, true));
                acp.addACL(acl);
                session.setACP(userProfileDocRef, acp, true);
                session.save();
            }
        }

        public DocumentModel getOrCreate() {
            if (session.hasPermission(userWorkspace.getRef(), ADD_CHILDREN)) {
                run();
            } else {
                runUnrestricted();
            }
            return session.getDocument(userProfileDocRef);
        }
    }

    @Override
    public void clearCache() {
        profileUidCache.invalidateAll();
    }

    @Override
    public ImporterConfig getImporterConfig() {
        return this.<ImporterConfig> getRegistryContribution(CONFIG_EP).orElse(null);
    }

    @Override
    public int getApplicationStartedOrder() {
        return 101; // after RepositoryService
    }

    @Override
    public void afterRuntimeStart(ComponentManager mgr, boolean isResume) {
        ImporterConfig config = getImporterConfig();
        if (config == null || config.getDataFileName() == null) {
            return;
        }
        // needs to run after RepositoryInitializationHandlers, run by RepositoryService
        scheduleImport();
    }

    protected void scheduleImport() {
        WorkManager wm = Framework.getService(WorkManager.class);
        if (wm != null) {
            wm.schedule(new UserProfileImporterWork(), true);
        }
    }

}

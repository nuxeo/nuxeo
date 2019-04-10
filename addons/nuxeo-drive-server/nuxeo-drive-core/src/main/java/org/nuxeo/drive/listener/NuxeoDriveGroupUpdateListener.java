/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Post-commit asynchronous listener that handles group change events fired by the {@link UserManager}.
 * <p>
 * For all the documents carrying an ACL impacted by a changed group or one of its ancestors it fires the
 * {@link NuxeoDriveEvents#GROUP_UPDATED} event that is handled by the synchronous
 * {@link NuxeoDriveFileSystemDeletionListener}.
 *
 * @since 9.2
 */
public class NuxeoDriveGroupUpdateListener implements PostCommitFilteringEventListener {

    protected static final Logger log = LogManager.getLogger(NuxeoDriveGroupUpdateListener.class);

    @Override
    public boolean acceptEvent(Event event) {
        return event.getContext() != null && UserManagerImpl.USER_GROUP_CATEGORY.equals(
                event.getContext().getProperty(DocumentEventContext.CATEGORY_PROPERTY_KEY));
    }

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            EventContext context = event.getContext();
            if (context == null) {
                continue;
            }
            String groupName = (String) context.getProperty(UserManagerImpl.ID_PROPERTY_KEY);
            if (groupName == null) {
                continue;
            }
            log.debug("NuxeoDriveGroupUpdateListener handling {} event for group {}", event::getName, () -> groupName);
            List<String> groupNames = getAllGroupNames(groupName, context);
            handleUpdatedGroups(groupNames);
        }
    }

    /**
     * Returns a list containing the names of the given group and all its ancestor groups.
     */
    @SuppressWarnings("unchecked")
    protected List<String> getAllGroupNames(String groupName, EventContext context) {
        List<String> groupNames = new ArrayList<>();
        groupNames.add(groupName);
        // Get ancestor groups from the event context or compute them if not provided
        // and do it as system user in the local thread to access group directory
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(new SystemPrincipal(null), null, null);
        try {
            List<String> ancestorGroups = (List<String>) context.getProperty(
                    UserManagerImpl.ANCESTOR_GROUPS_PROPERTY_KEY);
            if (ancestorGroups != null) {
                groupNames.addAll(ancestorGroups);
            } else {
                groupNames.addAll(Framework.getService(UserManager.class).getAncestorGroups(groupName));
            }
        } finally {
            loginStack.pop();
        }
        return groupNames;
    }

    protected void handleUpdatedGroups(List<String> groupNames) {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            CoreInstance.doPrivileged(repositoryName, (CoreSession session) -> {
                DocumentModelList impactedDocuments = getImpactedDocuments(session, groupNames);
                impactedDocuments.forEach(doc -> fireGroupUpdatedEvent(session, doc));
            });
        }
    }

    /**
     * Returns the list of documents carrying an ACL impacted by one of the given group names.
     */
    protected DocumentModelList getImpactedDocuments(CoreSession session, List<String> groupNames) {
        String groups = groupNames.stream().map(NXQL::escapeString).collect(Collectors.joining(","));
        String query = "SELECT * FROM Document WHERE ecm:isTrashed = 0 AND ecm:isVersion = 0 AND ecm:acl/*/principal IN ("
                + groups + ")";
        return session.query(query);
    }

    protected void fireGroupUpdatedEvent(CoreSession session, DocumentModel source) {
        EventContext context = new DocumentEventContext(session, session.getPrincipal(), source);
        Event event = context.newEvent(NuxeoDriveEvents.GROUP_UPDATED);
        Framework.getService(EventService.class).fireEvent(event);
    }

}

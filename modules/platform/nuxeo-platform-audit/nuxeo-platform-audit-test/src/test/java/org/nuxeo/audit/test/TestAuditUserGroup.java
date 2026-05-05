/*
 * (C) Copyright 2017-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 *     Kevin Leturc
 */
package org.nuxeo.audit.test;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.usermanager.UserManagerImpl.GROUPCREATED_EVENT_ID;
import static org.nuxeo.ecm.platform.usermanager.UserManagerImpl.GROUPDELETED_EVENT_ID;
import static org.nuxeo.ecm.platform.usermanager.UserManagerImpl.GROUPMODIFIED_EVENT_ID;
import static org.nuxeo.ecm.platform.usermanager.UserManagerImpl.USERCREATED_EVENT_ID;
import static org.nuxeo.ecm.platform.usermanager.UserManagerImpl.USERDELETED_EVENT_ID;
import static org.nuxeo.ecm.platform.usermanager.UserManagerImpl.USERMODIFIED_EVENT_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.provider.LatestCreatedUsersOrGroupsPageProvider;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.test.UserManagerFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test User and Group auditing.
 */
@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, CoreFeature.class, UserManagerFeature.class })
public class TestAuditUserGroup {

    @Inject
    protected AuditBackend backend;

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pps;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    public void testAuditEntriesForUserCUD() {
        List<LogEntry> entries;

        String userName = "testUser";

        // test user_created event
        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, USERCREATED_EVENT_ID)));
        assertEquals(0, entries.size());

        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userName);
        newUser = userManager.createUser(newUser);
        transactionalFeature.nextTransaction();

        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, USERCREATED_EVENT_ID)));
        assertEquals(1, entries.size());

        // test user_modified event
        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, USERMODIFIED_EVENT_ID)));
        assertEquals(0, entries.size());

        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", "StaticGroup");
        userManager.createGroup(groupModel);

        List<String> staticGroups = new ArrayList<>();
        staticGroups.add("StaticGroup");
        newUser = userManager.getUserModel(userName);
        newUser.setProperty("user", "groups", staticGroups);
        userManager.updateUser(newUser);
        transactionalFeature.nextTransaction();

        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, USERMODIFIED_EVENT_ID)));
        assertEquals(1, entries.size());

        // test user_deleted event
        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, USERDELETED_EVENT_ID)));
        assertEquals(0, entries.size());

        userManager.deleteUser(newUser);
        transactionalFeature.nextTransaction();

        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, USERDELETED_EVENT_ID)));
        assertEquals(1, entries.size());
    }

    @Test
    public void testAuditEntriesForGroupCUD() {
        List<LogEntry> entries;

        String groupName = "testGroup";

        // test group_created event
        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, GROUPCREATED_EVENT_ID)));
        assertEquals(0, entries.size());

        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", groupName);
        groupModel = userManager.createGroup(groupModel);
        transactionalFeature.nextTransaction();

        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, GROUPCREATED_EVENT_ID)));
        assertEquals(1, entries.size());

        // test group_modified event
        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, GROUPMODIFIED_EVENT_ID)));
        assertEquals(0, entries.size());

        userManager.updateGroup(groupModel);
        transactionalFeature.nextTransaction();

        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, GROUPMODIFIED_EVENT_ID)));
        assertEquals(1, entries.size());

        // test group_deleted event
        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, GROUPDELETED_EVENT_ID)));
        assertEquals(0, entries.size());

        userManager.deleteGroup(groupModel);
        transactionalFeature.nextTransaction();

        entries = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_EVENT_ID, GROUPDELETED_EVENT_ID)));
        assertEquals(1, entries.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveLatestCreatedUsersOrGroups() {

        String userName = "testUser";
        String groupName = "testGroup";

        final long LIMIT = 10L;
        for (int i = 0; i < LIMIT; i++) {
            if (i % 2 == 0) {
                DocumentModel newUser = userManager.getBareUserModel();
                newUser.setProperty("user", "username", userName + i);
                newUser = userManager.createUser(newUser);
            } else {
                DocumentModel groupModel = userManager.getBareGroupModel();
                groupModel.setProperty("group", "groupname", groupName + i);
                groupModel = userManager.createGroup(groupModel);
            }
        }
        transactionalFeature.nextTransaction();

        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider(
                LatestCreatedUsersOrGroupsPageProvider.LATEST_CREATED_USERS_OR_GROUPS_PROVIDER, null, LIMIT, 0L, props,
                session.getRootDocument().getId());

        List<DocumentModel> latestCreatedUsers = (List<DocumentModel>) pp.getCurrentPage();

        assertEquals(LIMIT, latestCreatedUsers.size());
    }

}

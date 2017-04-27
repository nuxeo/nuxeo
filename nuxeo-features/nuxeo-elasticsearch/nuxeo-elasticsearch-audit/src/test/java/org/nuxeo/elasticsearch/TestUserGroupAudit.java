/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Guillaume Renard"
 */

package org.nuxeo.elasticsearch;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.pageprovider.LatestCreatedUsersOrGroupsPageProvider;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test User and Group auditing.
 *
 * @since 8.2
 */
@Deploy({ "org.nuxeo.runtime.metrics", "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit",
        "org.nuxeo.ecm.platform.uidgen.core", "org.nuxeo.elasticsearch.seqgen",
        "org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit", "org.nuxeo.admin.center" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class, PlatformFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.audit:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:audit-test-contrib.xml" })
public class TestUserGroupAudit {

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected UserManager userManager;

    @Inject
    protected PageProviderService pps;

    @Inject
    protected CoreSession session;

    @Before
    public void setupIndex() throws Exception {
        // make sure that the audit bulker don't drain pending log entries while we reset the index
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);
    }

    @Test
    public void testAuditEntriesForUserCUD() throws Exception {
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> entries = null;

        String userName = "testUser";

        entries = reader.queryLogs(new String[] { UserManagerImpl.USERCREATED_EVENT_ID }, null);
        assertEquals(0, entries.size());
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userName);
        newUser = userManager.createUser(newUser);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.USERCREATED_EVENT_ID }, null);
        assertEquals(1, entries.size());

        entries = reader.queryLogs(new String[] { UserManagerImpl.USERMODIFIED_EVENT_ID }, null);
        assertEquals(0, entries.size());
        List<String> staticGroups = new ArrayList<String>();
        staticGroups.add("StaticGroup");
        newUser = userManager.getUserModel(userName);
        newUser.setProperty("user", "groups", staticGroups);
        userManager.updateUser(newUser);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.USERMODIFIED_EVENT_ID }, null);
        assertEquals(1, entries.size());

        entries = reader.queryLogs(new String[] { UserManagerImpl.USERDELETED_EVENT_ID }, null);
        assertEquals(0, entries.size());
        userManager.deleteUser(newUser);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.USERDELETED_EVENT_ID }, null);
        assertEquals(1, entries.size());
    }

    @Test
    public void testAuditEntriesForGroupCUD() throws Exception {
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> entries = null;

        String groupName = "testGroup";

        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPCREATED_EVENT_ID }, null);
        assertEquals(0, entries.size());
        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", groupName);
        groupModel = userManager.createGroup(groupModel);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPCREATED_EVENT_ID }, null);
        assertEquals(1, entries.size());

        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPMODIFIED_EVENT_ID }, null);
        assertEquals(0, entries.size());
        userManager.updateGroup(groupModel);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPMODIFIED_EVENT_ID }, null);
        assertEquals(1, entries.size());

        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPDELETED_EVENT_ID }, null);
        assertEquals(0, entries.size());
        userManager.deleteGroup(groupModel);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPDELETED_EVENT_ID }, null);
        assertEquals(1, entries.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetrieveLatestCreatedUsersOrGroups() throws Exception {

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

        LogEntryGen.flushAndSync();

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider(
                LatestCreatedUsersOrGroupsPageProvider.LATEST_CREATED_USERS_OR_GROUPS_PROVIDER, null, LIMIT, 0L, props,
                session.getRootDocument().getId());

        List<DocumentModel> latestCreatedUsers = (List<DocumentModel>) pp.getCurrentPage();

        assertEquals(LIMIT, latestCreatedUsers.size());
    }

}

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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
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
@Deploy({ "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit", "org.nuxeo.ecm.platform.uidgen.core",
    "org.nuxeo.elasticsearch.seqgen",
    "org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml",
    "org.nuxeo.elasticsearch.audit" })
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

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void testAuditEntriesForUserCUD() throws Exception {
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> entries = null;

        String userName = "testUser";

        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userName);
        newUser = userManager.createUser(newUser);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.USERCREATED_EVENT_ID}, null);
        assertEquals(1, entries.size());

        List<String> staticGroups = new ArrayList<String>();
        staticGroups.add("StaticGroup");
        newUser = userManager.getUserModel(userName);
        newUser.setProperty("user", "groups", staticGroups);
        userManager.updateUser(newUser);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.USERMODIFIED_EVENT_ID}, null);
        assertEquals(1, entries.size());

        userManager.deleteUser(newUser);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.USERDELETED_EVENT_ID}, null);
        assertEquals(1, entries.size());
    }

    @Test
    public void testAuditEntriesForGroupCUD() throws Exception {
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> entries = null;

        String groupName = "testGroup";

        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", groupName);
        groupModel = userManager.createGroup(groupModel);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPCREATED_EVENT_ID}, null);
        assertEquals(1, entries.size());

        userManager.updateGroup(groupModel);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPMODIFIED_EVENT_ID}, null);
        assertEquals(1, entries.size());

        userManager.deleteGroup(groupModel);
        LogEntryGen.flushAndSync();
        entries = reader.queryLogs(new String[] { UserManagerImpl.GROUPDELETED_EVENT_ID}, null);
        assertEquals(1, entries.size());
    }

}

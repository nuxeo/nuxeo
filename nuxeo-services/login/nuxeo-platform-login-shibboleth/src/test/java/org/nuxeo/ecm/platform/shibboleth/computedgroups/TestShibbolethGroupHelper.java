/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth.computedgroups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethGroupHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.ecm.platform.usermanager", "org.nuxeo.ecm.platform.login.shibboleth" })
@Deploy("org.nuxeo.ecm.platform.login.shibboleth:OSGI-INF/test-sql-directory.xml")
public class TestShibbolethGroupHelper {

    protected static final String CORRECT_EL = "empty currentUser";

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testCreateGroup() throws Exception {
        assertEquals(0, ShibbolethGroupHelper.getGroups().size());
        DocumentModel group = ShibbolethGroupHelper.getBareGroupModel(session);

        group.setPropertyValue("shibbolethGroup:groupName", "group1");
        group.setPropertyValue("shibbolethGroup:expressionLanguage", CORRECT_EL);
        ShibbolethGroupHelper.createGroup(group);

        assertEquals(1, ShibbolethGroupHelper.getGroups().size());
        deleteShibbGroups();
        assertEquals(0, ShibbolethGroupHelper.getGroups().size());
    }

    @Test
    public void testSearchGroup() throws Exception {
        createShibbGroup("group2");
        createShibbGroup("group3");
        createShibbGroup("group4");
        createShibbGroup("test");
        createShibbGroup("group6");

        assertEquals(1, ShibbolethGroupHelper.searchGroup("test").size());
        assertEquals(5, ShibbolethGroupHelper.searchGroup("").size());
        assertEquals(4, ShibbolethGroupHelper.searchGroup("group%").size());
        assertEquals(4, ShibbolethGroupHelper.searchGroup("group").size());
        deleteShibbGroups();
    }

    @Test
    public void testGetReference() throws Exception {
        DocumentModel group = userManager.getBareGroupModel();
        group.setPropertyValue("group:groupname", "testRef");
        group = userManager.createGroup(group);

        assertEquals("testRef", group.getId());

        DocumentModel shibbGroup = createShibbGroup("refShib");
        List<String> ref = new ArrayList<>();
        assertEquals("refShib", shibbGroup.getId());
        ref.add(shibbGroup.getId());

        group.setProperty(userManager.getGroupSchemaName(), userManager.getGroupSubGroupsField(), ref);
        userManager.updateGroup(group);
        session.save();

        Directory dir = directoryService.getDirectory(userManager.getGroupDirectoryName());
        assertNotNull(dir.getReference(userManager.getGroupSubGroupsField()));

        Session ses = directoryService.open(userManager.getGroupDirectoryName());
        DocumentModel tmp = ses.getEntry("testRef");
        @SuppressWarnings("unchecked")
        List<String> subs = (List<String>) tmp.getProperty(userManager.getGroupSchemaName(),
                userManager.getGroupSubGroupsField());
        assertNotNull(subs);
        assertEquals(1, subs.size());

        Reference dirRef = dir.getReference(userManager.getGroupSubGroupsField());

        assertTrue(dirRef.getTargetIdsForSource("testRef").size() > 0);
        assertTrue(dirRef.getSourceIdsForTarget("refShib").size() > 0);
        assertEquals("testRef", dirRef.getSourceIdsForTarget("refShib").get(0));
        deleteShibbGroups();
    }

    @Test
    public void testSubGroups() throws Exception {
        DocumentModel group = userManager.getBareGroupModel();
        group.setPropertyValue("group:groupname", "trueGroup1");
        group = userManager.createGroup(group);
        assertEquals("trueGroup1", group.getId());

        DocumentModel group2 = userManager.getBareGroupModel();
        group2.setPropertyValue("group:groupname", "trueGroup2");
        group2 = userManager.createGroup(group2);

        DocumentModel group3 = userManager.getBareGroupModel();
        group3.setPropertyValue("group:groupname", "trueGroup3");
        group3 = userManager.createGroup(group3);

        List<String> subGroup = new ArrayList<>();
        List<String> subGroup2 = new ArrayList<>();

        DocumentModel shibGroup = createShibbGroup("members");
        subGroup.add(shibGroup.getId());
        subGroup2.add(shibGroup.getId());

        assertNotNull(shibGroup.getId());

        shibGroup = createShibbGroup("shibbou");
        subGroup.add(shibGroup.getId());

        shibGroup = createShibbGroup("group7");
        subGroup.add(shibGroup.getId());
        subGroup2.add(shibGroup.getId());

        shibGroup = createShibbGroup("group73");
        subGroup.add(shibGroup.getId());

        shibGroup = createShibbGroup("participant");
        subGroup.add(shibGroup.getId());
        subGroup2.add(shibGroup.getId());

        group.setProperty(userManager.getGroupSchemaName(), userManager.getGroupSubGroupsField(), subGroup);
        group2.setProperty(userManager.getGroupSchemaName(), userManager.getGroupSubGroupsField(), subGroup2);

        userManager.updateGroup(group);
        userManager.updateGroup(group2);

        session.save();

        List<String> parent = ShibbolethGroupHelper.getParentsGroups("shibbou");

        assertNotNull(parent);
        assertEquals(1, parent.size());
        assertEquals("trueGroup1", parent.get(0));

        parent = ShibbolethGroupHelper.getParentsGroups("group7");

        assertNotNull(parent);
        assertEquals(2, parent.size());
        deleteShibbGroups();
    }

    protected DocumentModel createShibbGroup(String name) throws Exception {
        DocumentModel group = ShibbolethGroupHelper.getBareGroupModel(session);
        group.setPropertyValue("shibbolethGroup:groupName", name);
        group.setPropertyValue("shibbolethGroup:expressionLanguage", CORRECT_EL);

        group = ShibbolethGroupHelper.createGroup(group);
        session.save();
        return group;
    }

    protected void deleteShibbGroups() throws Exception {
        for (DocumentModel group : ShibbolethGroupHelper.getGroups()) {
            ShibbolethGroupHelper.deleteGroup(group);
        }
        session.save();
    }

}

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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupCreationFormPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupEditFormPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupViewTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupsTabSubPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 8.2
 */
public class ITGroupsTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.createUser("jdoe", "jdoe1", "John", "Doe", "Nuxeo", "dev@null", "members");
        RestHelper.createUser("jsmith", "jsmith1", "Jim", "Smith", "Nuxeo", "dev@null", "members");
        RestHelper.createUser("bree", "bree1", "Bree", "Van de Kaamp", "Nuxeo", "dev@null", "members");
        RestHelper.createUser("lbramard", "lbramard1", "Lucien", "Bramard", "Nuxeo", "dev@null", "members");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testCreateViewDeleteGroup() throws Exception {
        GroupsTabSubPage groupsTab = login().getAdminCenter().getUsersGroupsHomePage().getGroupsTab();
        assertEquals("Group created.",
                groupsTab.getGroupCreatePage()
                         .createGroup("Johns", null, new String[] { "jdoe", "jsmith", "bree" }, null)
                         .getInfoFeedbackMessage());
        GroupViewTabSubPage groupView = asPage(GroupViewTabSubPage.class);
        assertEquals("Johns", groupView.getGroupName());
        GroupEditFormPage groupEdit = groupView.getEditGroupTab();
        List<String> members = groupEdit.getMembers();
        assertEquals(3, members.size());
        assertEquals("John Doe \njdoe", members.get(0));
        assertEquals("Jim Smith \njsmith", members.get(1));
        assertEquals("Bree Van de Kaamp \nbree", members.get(2));
        groupEdit.addMember("lbramard").save().getEditGroupTab();
        members = groupEdit.getMembers();
        assertEquals(4, members.size());
        assertEquals("John Doe \njdoe", members.get(0));
        assertEquals("Jim Smith \njsmith", members.get(1));
        assertEquals("Bree Van de Kaamp \nbree", members.get(2));
        assertEquals("Lucien Bramard \nlbramard", members.get(3));

        assertTrue(groupView.backToTheList().searchGroup("Johns").isGroupFound("Johns"));

        // create again
        asPage(GroupsTabSubPage.class).getGroupCreatePage().createGroup("Johns", null, null, null);
        assertEquals("Group already exists.", asPage(GroupCreationFormPage.class).getErrorFeedbackMessage());

        asPage(GroupCreationFormPage.class).cancelCreation().searchGroup("Johns").viewGroup("Johns").deleteGroup();
        assertFalse(asPage(GroupsTabSubPage.class).searchGroup("Johns").isGroupFound("Johns"));

        logout();
    }

}

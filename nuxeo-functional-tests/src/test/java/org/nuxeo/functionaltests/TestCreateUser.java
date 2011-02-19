/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.UsersTabSubPage;
import org.nuxeo.functionaltests.waitfor.ElementNotFoundException;

/**
 * Create a user in Nuxeo DM.
 */
public class TestCreateUser extends AbstractTest {

    @Test
    public void testCreateUser() throws ElementNotFoundException {
        String username = "test_user3";
        DocumentBasePage page;
        UsersTabSubPage userTab = login().getHeaderLinks().goToUserManagementPage().getUsersTab();
        userTab = userTab.searchUser(username);
        if (!userTab.isUserFound(username)) {
            page = userTab.getUserCreatePage().createUser(username,
                    "firstname1", "lastname1", "company1", "email1",
                    "test_user1", "members");
            assertEquals(page.getFeedbackMessage(), "User created");
        }
    }

}

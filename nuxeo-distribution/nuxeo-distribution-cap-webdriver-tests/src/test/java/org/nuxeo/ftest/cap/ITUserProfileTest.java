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
 *     Thomas Roger
 *
 */

package org.nuxeo.ftest.cap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.UserHomePage;
import org.nuxeo.functionaltests.pages.profile.EditProfilePage;
import org.nuxeo.functionaltests.pages.profile.ProfilePage;
import org.openqa.selenium.By;

/**
 * @since 8.2
 */
public class ITUserProfileTest extends AbstractTest {

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testUserProfile() throws DocumentBasePage.UserNotConnectedException {
        DocumentBasePage page = login(TEST_USERNAME, TEST_PASSWORD);
        UserHomePage userHome = page.getUserHome();
        ProfilePage profilePage = userHome.goToProfile();
        EditProfilePage editProfilePage = profilePage.getEditProfilePage();
        editProfilePage.setEmail("devnull@nuxeo.com");
        editProfilePage.setBirthDate("3/25/1982");
        editProfilePage.setPhoneNumber("555-4321");
        editProfilePage.saveProfile();
        Locator.findElement(By.xpath("//span[text() = '3/25/1982']"));
        Locator.findElement(By.xpath("//span[text() = '555-4321']"));
    }

}

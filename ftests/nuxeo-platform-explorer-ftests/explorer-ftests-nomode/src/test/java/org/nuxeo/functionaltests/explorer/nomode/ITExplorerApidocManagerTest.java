/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.functionaltests.explorer.nomode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.UploadFragment;

/**
 * Tests features for {@link SecurityHelper#APIDOC_MANAGERS_GROUP} members.
 *
 * @since 11.2
 */
public class ITExplorerApidocManagerTest extends ITExplorerAdminTest {

    @Override
    @Before
    public void before() {
        RestHelper.createGroupIfDoesNotExist(SecurityHelper.DEFAULT_APIDOC_MANAGERS_GROUP, "Apidoc Managers", null,
                null);
        RestHelper.createUserIfDoesNotExist(MANAGER_USERNAME, TEST_PASSWORD, null, null, null, null,
                SecurityHelper.DEFAULT_APIDOC_MANAGERS_GROUP);
        super.before();
    }

    @Override
    @After
    public void after() {
        super.after();
        RestHelper.cleanup();
    }

    @Override
    protected void doLogin() {
        getLoginPage().login(MANAGER_USERNAME, TEST_PASSWORD);
    }

    @Override
    @Test
    public void testDistribAdminPage() {
        open(ExplorerHomePage.URL);
        asPage(ExplorerHomePage.class).checkManageLink();
        open(DistribAdminPage.URL);
        DistribAdminPage page = asPage(DistribAdminPage.class);
        page.check();
        // since 11.2: cannot save anymore
        page.checkCannotSave();
    }

    @Override
    @Test
    public void testHomePageLiveDistrib() {
        ExplorerHomePage home = goHome();
        home.check();
        // since 11.2: cannot see current live distrib anymore
        home.checkNoCurrentDistrib();
        UploadFragment.checkCanSee();
    }

    @Test
    @Override
    public void testLiveDistribExportAndImport() {
        String distribName = "my-server";
        open(DistribAdminPage.URL);
        asPage(DistribAdminPage.class).checkCannotSave();
        // log as admin to perform export of live distrib first
        doLogout();
        loginAsAdmin();
        String distribId = checkLiveDistribExport(distribName);
        doLogout();
        doLogin();
        checkLiveDistribImport(distribId);
    }

    @Test
    @Override
    public void testLivePartialDistribExportAndImport() {
        String distribName = "my-partial-server";
        open(DistribAdminPage.URL);
        asPage(DistribAdminPage.class).checkCannotSave();
        // log as admin to perform export of live distrib first
        doLogout();
        loginAsAdmin();
        String distribId = checkLivePartialDistribExport(distribName);
        doLogout();
        doLogin();
        checkLivePartialDistribImport(distribName, distribId);
    }

}

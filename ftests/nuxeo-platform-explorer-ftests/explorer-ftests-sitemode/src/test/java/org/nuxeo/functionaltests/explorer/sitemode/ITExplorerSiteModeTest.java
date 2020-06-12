/*
 * (C) Copyright 2014-2020 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.sitemode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.DistributionHomePage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.UploadFragment;

/**
 * Test explorer in site mode.
 *
 * @since 11.2
 */
public class ITExplorerSiteModeTest extends AbstractExplorerSiteModeTest {

    @Before
    public void before() {
        RestHelper.createUserIfDoesNotExist(READER_USERNAME, TEST_PASSWORD, null, null, null, null, null);
        doLogin();
    }

    @After
    public void after() {
        doLogout();
        RestHelper.cleanup();
    }

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() {
        goHome();
    }

    /**
     * Checks the distrib admin page is hidden to any non-admin user.
     */
    @Test
    public void testDistribAdminPage() {
        openAndCheck(DistribAdminPage.URL, true);
    }

    @Test
    public void testHomePageCurrentDistrib() {
        // since 11.2: does not redirect to current live distrib anymore, only available to admins
        openAndCheck(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_CURRENT), true);
    }

    @Test
    public void testHomePageLatestDistrib() {
        open(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_LATEST));
        // persisted distrib redirection
        asPage(DistributionHomePage.class).check();
    }

    @Test
    public void testSampleDistrib() {
        ExplorerHomePage home = goHome();
        home.check();
        home.checkFirstPersistedDistrib(DISTRIB_NAME, DISTRIB_VERSION);
        UploadFragment.checkCannotSee();

        String distribId = getDistribId(DISTRIB_NAME, DISTRIB_VERSION);
        asPage(ExplorerHomePage.class).checkPersistedDistrib(distribId);
        checkDistrib(distribId, true, SAMPLE_BUNDLE_GROUP, true);
    }

}

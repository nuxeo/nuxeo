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
package org.nuxeo.functionaltests.explorer.nomode;

import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.LiveSimplePage;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerTest;

/**
 * Checks access to some explorer pages without any persisted distributions.
 *
 * @since 11.2
 */
public class ITExplorerNoInitTest extends AbstractExplorerTest {

    @Before
    public void before() {
        RestHelper.createGroupIfDoesNotExist(SecurityHelper.DEFAULT_APIDOC_MANAGERS_GROUP, "Apidoc Managers", null,
                null);
        RestHelper.createUserIfDoesNotExist(MANAGER_USERNAME, TEST_PASSWORD, null, null, null, null,
                SecurityHelper.DEFAULT_APIDOC_MANAGERS_GROUP);
        RestHelper.createUserIfDoesNotExist(READER_USERNAME, TEST_PASSWORD, null, null, null, null, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    protected void checkPagesNoMode(boolean isAdmin) {
        open(ExplorerHomePage.URL);
        ExplorerHomePage home = asPage(ExplorerHomePage.class);
        home.check();
        assumeTrue("TODO NXP-29050: current live distrib not correctly protected", false);
        if (isAdmin) {
            home.checkCurrentDistrib();
        } else {
            home.checkNoDistrib();
        }
        openAndCheck(LiveSimplePage.URL, !isAdmin);
        openAndCheck(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_CURRENT), !isAdmin);
        // no latest distrib if not using "nuxeo platform" title
        openAndCheck(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_LATEST), true);
        openAndCheck(String.format("%s%s/", ExplorerHomePage.URL, "foo-10.10"), true);
        checkLiveJson(!isAdmin);
    }

    @Test
    public void testPagesByAdmin() {
        try {
            loginAsAdmin();
            open(DistribAdminPage.URL);
            asPage(DistribAdminPage.class).check();
            checkPagesNoMode(true);
        } finally {
            doLogout();
        }
    }

    @Test
    public void testPagesByManager() {
        try {
            getLoginPageStatic().login(MANAGER_USERNAME, TEST_PASSWORD);
            open(DistribAdminPage.URL);
            asPage(DistribAdminPage.class).check();
            checkPagesNoMode(false);
        } finally {
            doLogout();
        }
    }

    @Test
    public void testPagesByReader() {
        try {
            doLogin();
            openAndCheck(DistribAdminPage.URL, true);
            checkPagesNoMode(false);
        } finally {
            doLogout();
        }
    }

}

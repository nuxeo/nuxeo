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
package org.nuxeo.functionaltests.explorer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ContributionArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ExtensionPointArtifactPage;

/**
 * Test explorer "adm" "simple" webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerTest extends AbstractExplorerTest {

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
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
        open(DistribAdminPage.URL);
        assertEquals("", driver.getTitle());
        assertEquals("", driver.getPageSource());
    }

    @Test
    public void testHomePageLiveDistrib() {
        ExplorerHomePage home = goHome();
        home.check();

        home.clickOn(home.currentExtensionPoints);
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkTitle("All Extension Points");
        header.checkSelectedTab(header.extensionPoints);

        header.goHome().clickOn(home.currentContributions);
        header = asPage(DistributionHeaderFragment.class);
        header.checkTitle("All Contributions");
        header.checkSelectedTab(header.contributions);

        header.goHome().clickOn(home.currentOperations);
        header = asPage(DistributionHeaderFragment.class);
        header.checkTitle("All Operations");
        header.checkSelectedTab(header.operations);

        header.goHome().clickOn(home.currentServices);
        header = asPage(DistributionHeaderFragment.class);
        header.checkTitle("All Services");
        header.checkSelectedTab(header.services);
    }

    @Test
    public void testExtensionPoints() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentExtensionPoints);
        checkExtensionPoints();
    }

    @Test
    public void testContributions() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentContributions);
        checkContributions();
    }

    @Test
    public void testServices() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentServices);
        checkServices();
    }

    @Test
    public void testOperations() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentOperations);
        checkOperations();
    }

    @Test
    public void testComponents() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentExtensionPoints);
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header = header.navigateTo(header.components);
        header.checkSelectedTab(header.components);
        checkComponents();
    }

    @Test
    public void testBundles() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentExtensionPoints);
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header = header.navigateTo(header.bundles);
        header.checkSelectedTab(header.bundles);
        checkBundles();
    }

    @Test
    public void testOverrideContribution() throws IOException {
        open(String.format("%s%s/%s/%s", ExplorerHomePage.URL, ApiBrowserConstants.DISTRIBUTION_ALIAS_CURRENT,
                ApiBrowserConstants.VIEW_CONTRIBUTION, "org.nuxeo.apidoc.listener.contrib--listener"));
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        apage.toggleGenerateOverride();
        storeWindowHandle();
        apage.doGenerateOverride();
        switchToNewWindow();
        String expected = AbstractExplorerTest.getReferenceContent("data/override_reference.xml");
        assertEquals(expected, driver.getPageSource());
        switchBackToPreviousWindow();
    }

    @Test
    public void testOverrideContributionFromExtensionPoint() throws IOException {
        open(String.format("%s%s/%s/%s", ExplorerHomePage.URL, ApiBrowserConstants.DISTRIBUTION_ALIAS_CURRENT,
                ApiBrowserConstants.VIEW_EXTENSIONPOINT, "org.nuxeo.ecm.core.event.EventServiceComponent--listener"));
        ExtensionPointArtifactPage apage = asPage(ExtensionPointArtifactPage.class);
        storeWindowHandle();
        apage.generateOverride("org.nuxeo.apidoc.listener.contrib--listener");
        switchToNewWindow();
        String expected = AbstractExplorerTest.getReferenceContent("data/override_xp_reference.xml");
        assertEquals(expected, driver.getPageSource());
        switchBackToPreviousWindow();
    }

}
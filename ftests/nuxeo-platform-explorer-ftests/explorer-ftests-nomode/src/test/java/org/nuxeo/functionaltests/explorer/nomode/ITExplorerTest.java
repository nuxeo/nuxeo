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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.DistribAdminPage;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.nuxeo.functionaltests.explorer.pages.DistributionHomePage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.UploadFragment;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleGroupArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ComponentArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ContributionArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ExtensionPointArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.OperationArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ServiceArtifactPage;
import org.nuxeo.functionaltests.explorer.testing.AbstractExplorerTest;

/**
 * Test explorer "adm" "simple" webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerTest extends AbstractExplorerTest {

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
    public void testHomePageLiveDistrib() {
        ExplorerHomePage home = goHome();
        home.check();
        UploadFragment.checkCannotSee();

        home.goHome().clickOn(home.currentDistrib);
        DistributionHomePage dhome = asPage(DistributionHomePage.class);
        dhome.check();

        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        home = header.goHome();
        home.clickOn(home.currentExtensionPoints);
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
    public void testHomePageCurrentDistrib() {
        open(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_CURRENT));
        // current live distrib redirection
        asPage(DistributionHomePage.class).check();
    }

    @Test
    public void testHomePageLatestDistrib() {
        open(String.format("%s%s/", ExplorerHomePage.URL, SnapshotManager.DISTRIBUTION_ALIAS_LATEST));
        // current live distrib redirection
        asPage(DistributionHomePage.class).check();
    }

    /**
     * Non-regression test for NXP-29193.
     */
    @Test
    public void testHomePageInvalidDistrib() {
        open(String.format("%s%s/", ExplorerHomePage.URL, "foo-10.10"));
        // current live distrib redirection
        asPage(DistributionHomePage.class).check();
    }

    @Test
    public void testExtensionPoints() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentExtensionPoints);
        checkExtensionPoints(false, false);
    }

    @Test
    public void testExtensionPointsAlternative() {
        goToArtifact(ExtensionPointInfo.TYPE_NAME, "org.nuxeo.ecm.core.schema.TypeService--doctype");
        ExtensionPointArtifactPage apage = asPage(ExtensionPointArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testContributions() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentContributions);
        checkContributions(false, false);
    }

    @Test
    public void testContributionsAlternative() {
        goToArtifact(ExtensionInfo.TYPE_NAME, "org.nuxeo.apidoc.listener.contrib--listener");
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testServices() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentServices);
        checkServices(false, false);
    }

    @Test
    public void testServicesAlternative() {
        goToArtifact(ServiceInfo.TYPE_NAME, "org.nuxeo.ecm.platform.types.TypeManager");
        ServiceArtifactPage apage = asPage(ServiceArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testOperations() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentOperations);
        checkOperations(false, false);
    }

    @Test
    public void testOperationsAlternative() {
        goToArtifact(OperationInfo.TYPE_NAME, "FileManager.ImportWithMetaData");
        OperationArtifactPage apage = asPage(OperationArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testComponents() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentExtensionPoints);
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header = header.navigateTo(header.components);
        header.checkSelectedTab(header.components);
        checkComponents(false, false);
    }

    @Test
    public void testComponentsAlternative() {
        goToArtifact(ComponentInfo.TYPE_NAME, "org.nuxeo.ecm.automation.server.marshallers");
        ComponentArtifactPage apage = asPage(ComponentArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testBundles() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentExtensionPoints);
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header = header.navigateTo(header.bundles);
        header.checkSelectedTab(header.bundles);
        checkBundles(false, false);
    }

    @Test
    public void testBundlesAlternative() {
        goToArtifact(BundleInfo.TYPE_NAME, "org.nuxeo.apidoc.webengine");
        BundleArtifactPage apage = asPage(BundleArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testBundleGroups() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentDistrib);
        DistributionHomePage dhome = asPage(DistributionHomePage.class);
        dhome.clickOn(dhome.bundleGroups);
        checkBundleGroups(false, null, false);
    }

    @Test
    public void testBundleGroupsAlternative() {
        // check subgroup
        goToArtifact(BundleGroup.TYPE_NAME, "org.nuxeo.ecm.directory");
        BundleGroupArtifactPage apage = asPage(BundleGroupArtifactPage.class);
        apage.checkAlternative();
    }

    @Test
    public void testPackages() {
        ExplorerHomePage home = goHome();
        home.clickOn(home.currentDistrib);
        DistributionHomePage dhome = asPage(DistributionHomePage.class);
        dhome.clickOn(dhome.packages);
        checkPackages(false, false);
    }

    @Test
    public void testOverrideContribution() throws IOException {
        goToArtifact(ExtensionInfo.TYPE_NAME, "org.nuxeo.apidoc.listener.contrib--listener");
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        apage.toggleGenerateOverride();
        storeWindowHandle();
        apage.doGenerateOverride();
        switchToNewWindow();
        String expected = AbstractExplorerTest.getReferenceContent("data/override_reference.xml");
        assertEquals(expected, driver.getPageSource());
        switchBackToPreviousWindow();
    }

    /**
     * Non-regression test for NXP-19766.
     */
    @Test
    public void testOverrideContributionWithXMLComments() throws Exception {
        goToArtifact(ExtensionInfo.TYPE_NAME, "org.nuxeo.ecm.platform.comment.defaultPermissions--permissions");
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        apage.toggleGenerateOverride();
        storeWindowHandle();
        apage.doGenerateOverride();
        switchToNewWindow();
        String generatedXML = driver.getPageSource();
        assertNotNull(generatedXML);
        assertFalse(generatedXML.contains("parsererror"));
        switchBackToPreviousWindow();
    }

    @Test
    public void testOverrideContributionFromExtensionPoint() throws IOException {
        goToArtifact(ExtensionPointInfo.TYPE_NAME, "org.nuxeo.ecm.core.event.EventServiceComponent--listener");
        ExtensionPointArtifactPage apage = asPage(ExtensionPointArtifactPage.class);
        storeWindowHandle();
        apage.generateOverride("org.nuxeo.apidoc.listener.contrib--listener");
        switchToNewWindow();
        String expected = AbstractExplorerTest.getReferenceContent("data/override_xp_reference.xml");
        assertEquals(expected, driver.getPageSource());
        switchBackToPreviousWindow();
    }

}

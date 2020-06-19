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
package org.nuxeo.functionaltests.explorer.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.JavaScriptErrorCollector;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.nuxeo.functionaltests.explorer.pages.DistributionHomePage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.ListingFragment;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleGroupArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ComponentArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ContributionArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ExtensionPointArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.OperationArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.PackageArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ServiceArtifactPage;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * @since 11.1
 */
public abstract class AbstractExplorerTest extends AbstractTest {

    protected static String READER_USERNAME = TEST_USERNAME;

    protected static String MANAGER_USERNAME = "apidocmanager";

    public static LoginPage getLoginPageStatic() {
        return get(NUXEO_URL + "/logout", LoginPage.class);
    }

    protected static void loginAsAdmin() {
        getLoginPageStatic().login(ADMINISTRATOR, ADMINISTRATOR);
    }

    protected void doLogin() {
        getLoginPageStatic().login(TEST_USERNAME, TEST_PASSWORD);
    }

    protected static void doLogout() {
        // logout avoiding JS error check
        driver.get(NUXEO_URL + "/logout");
    }

    protected static void open(String url) {
        openAndCheck(url, false);
    }

    protected static void openAndCheck(String url, boolean checkIs404) {
        JavaScriptErrorCollector.from(driver).checkForErrors();
        driver.get(NUXEO_URL + url);
        if (checkIs404) {
            assertEquals("404 - Resource Not Found", driver.getTitle());
        } else {
            assertFalse("404 on URL: '" + driver.getCurrentUrl(),
                    driver.getTitle().contains(String.valueOf(HttpStatus.SC_NOT_FOUND)));
        }
    }

    protected static void cleanupPersistedDistributions() {
        RestHelper.deleteDocument(SnapshotPersister.Root_PATH + SnapshotPersister.Root_NAME);
    }

    protected static void cleanupPersistedDistribution(String name) {
        RestHelper.deleteDocument(SnapshotPersister.Root_PATH + SnapshotPersister.Root_NAME + "/" + name);
    }

    protected String getDistribId(String name, String version) {
        return String.format("%s-%s", name, version);
    }

    protected String getDistribExportName(String distribId) {
        return String.format("nuxeo-distribution-%s.zip", distribId);
    }

    protected ExplorerHomePage goHome() {
        open(ExplorerHomePage.URL);
        return asPage(ExplorerHomePage.class);
    }

    protected boolean hasNavigationHeader() {
        return true;
    }

    public void checkHomeLiveDistrib() {
        ExplorerHomePage home = asPage(ExplorerHomePage.class);
        home.clickOn(home.currentDistrib);
        DistributionHomePage dhome = asPage(DistributionHomePage.class);
        dhome.check();
        checkFirstDistrib();
    }

    public void checkHomeFirstPersistedDistrib() {
        ExplorerHomePage home = asPage(ExplorerHomePage.class);
        home.goHome().clickOn(home.firstPersistedDistrib);
        DistributionHomePage dhome = asPage(DistributionHomePage.class);
        dhome.check();
        checkFirstDistrib();
    }

    public void checkFirstDistrib() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        ExplorerHomePage home = header.goHome();
        home.clickOn(home.firstExtensionPoints);
        header.checkTitle("All Extension Points");
        header.checkSelectedTab(header.extensionPoints);

        header.goHome().clickOn(home.firstContributions);
        header = asPage(DistributionHeaderFragment.class);
        header.checkTitle("All Contributions");
        header.checkSelectedTab(header.contributions);

        header.goHome().clickOn(home.firstOperations);
        header = asPage(DistributionHeaderFragment.class);
        header.checkTitle("All Operations");
        header.checkSelectedTab(header.operations);

        header.goHome().clickOn(home.firstServices);
        header = asPage(DistributionHeaderFragment.class);
        header.checkTitle("All Services");
        header.checkSelectedTab(header.services);
    }

    protected void checkDistrib(String distribId, boolean partial, String partialVirtualGroup, boolean legacy) {
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_BUNDLEGROUPS);
        checkBundleGroups(partial, partialVirtualGroup, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_BUNDLES);
        checkBundles(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_COMPONENTS);
        checkComponents(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_EXTENSIONPOINTS);
        checkExtensionPoints(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_SERVICES);
        checkServices(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_CONTRIBUTIONS);
        checkContributions(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_OPERATIONS);
        checkOperations(partial, legacy);
        open(ExplorerHomePage.URL + distribId + "/" + ApiBrowserConstants.LIST_PACKAGES);
        checkPackages(partial, legacy);
    }

    protected void checkExtensionPoints(boolean partial, boolean legacy) {
        ListingFragment listing = asPage(ListingFragment.class);
        if (!partial) {
            listing.checkListing(-1, "actions",
                    "/viewExtensionPoint/org.nuxeo.ecm.platform.actions.ActionService--actions",
                    "ActionService - org.nuxeo.ecm.platform.actions.ActionService");
            listing = listing.filterOn("org.nuxeo.apidoc");
        }
        listing.checkListing(1, "plugins",
                "/viewExtensionPoint/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins",
                "SnapshotManagerComponent - org.nuxeo.apidoc.snapshot.SnapshotManagerComponent");

        listing.navigateToFirstItem();
        ExtensionPointArtifactPage apage = asPage(ExtensionPointArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.checkReference(partial, legacy);
    }

    protected void checkContributions(boolean partial, boolean legacy) {
        ListingFragment listing = asPage(ListingFragment.class);
        if (!partial) {
            listing.checkListing(-1, "cluster-config--configuration", "/viewContribution/cluster-config--configuration",
                    "configuration - org.nuxeo.runtime.cluster.ClusterService");
            listing = listing.filterOn("org.nuxeo.apidoc");
        }
        int nb = legacy ? 5 : 9;
        listing.checkListing(nb, "org.nuxeo.apidoc.adapterContrib--adapters",
                "/viewContribution/org.nuxeo.apidoc.adapterContrib--adapters",
                "adapters - org.nuxeo.ecm.core.api.DocumentAdapterService");

        listing.navigateToFirstItem();
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.checkReference(partial, legacy);
    }

    protected void checkServices(boolean partial, boolean legacy) {
        ListingFragment listing = asPage(ListingFragment.class);
        if (!partial) {
            listing.checkListing(-1, "ActionManager", "/viewService/org.nuxeo.ecm.platform.actions.ejb.ActionManager",
                    "org.nuxeo.ecm.platform.actions.ejb.ActionManager");
            listing = listing.filterOn("org.nuxeo.apidoc");
        }
        // toggle sort to check the SnapshotManager service
        listing = listing.toggleSort();
        listing.checkListing(2, "SnapshotManager", "/viewService/org.nuxeo.apidoc.snapshot.SnapshotManager",
                "org.nuxeo.apidoc.snapshot.SnapshotManager");

        listing.navigateToFirstItem();
        ServiceArtifactPage apage = asPage(ServiceArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.checkReference(partial, legacy);
    }

    protected void checkOperations(boolean partial, boolean legacy) {
        ListingFragment listing = asPage(ListingFragment.class);
        if (partial) {
            listing.checkListing(0, null, null, null);
            return;
        }

        listing.checkListing(-1, "acceptComment", "/viewOperation/acceptComment", "CHAIN acceptComment");

        listing = listing.filterOn("Document.AddFacet");
        listing.checkListing(1, "Add Facet", "/viewOperation/Document.AddFacet",
                "DOCUMENT Document.AddFacet\n" + "Alias Document.AddFacet");

        listing.navigateToFirstItem();
        OperationArtifactPage apage = asPage(OperationArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.checkReference(partial, legacy);
    }

    protected void checkComponents(boolean partial, boolean legacy) {
        ListingFragment listing = asPage(ListingFragment.class);
        if (!partial) {
            listing.checkListing(-1, "actions.ActionService",
                    "/viewComponent/org.nuxeo.ecm.platform.actions.ActionService",
                    "JAVA org.nuxeo.ecm.platform.actions.ActionService");
            listing = listing.filterOn("org.nuxeo.apidoc");
        }
        listing = listing.toggleSort();
        listing.checkListing(6, "apidoc.snapshot.SnapshotManagerComponent",
                "/viewComponent/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                "JAVA org.nuxeo.apidoc.snapshot.SnapshotManagerComponent");

        listing.navigateToFirstItem();
        ComponentArtifactPage apage = asPage(ComponentArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.checkReference(partial, legacy);
    }

    protected void checkBundles(boolean partial, boolean legacy) {
        ListingFragment listing = asPage(ListingFragment.class);
        if (!partial) {
            listing.checkListing(-1, "org.nuxeo.admin.center", "/viewBundle/org.nuxeo.admin.center", null);
            listing = listing.filterOn("org.nuxeo.apidoc");
        }
        listing.checkListing(3, "org.nuxeo.apidoc.core", "/viewBundle/org.nuxeo.apidoc.core", null);

        listing.navigateToFirstItem();
        BundleArtifactPage apage = asPage(BundleArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.checkReference(partial, legacy);
    }

    protected void checkBundleGroups(boolean partial, String partialVirtualGroup, boolean legacy) {
        if (partial) {
            Locator.findElementWaitUntilEnabledAndClick(By.linkText(partialVirtualGroup));
        } else {
            Locator.findElementWaitUntilEnabledAndClick(By.linkText("org.nuxeo.ecm.platform"));
        }
        BundleGroupArtifactPage apage = asPage(BundleGroupArtifactPage.class);
        apage.checkReference(partial, legacy);
    }

    protected void checkPackages(boolean partial, boolean legacy) {
        ListingFragment listing = asPage(ListingFragment.class);
        if (legacy) {
            // no packages on legacy
            listing.checkListing(0, null, null, null);
        } else {
            listing.checkListing(1, "Platform Explorer", "/viewPackage/platform-explorer", "ADDON platform-explorer");

            listing.navigateToFirstItem();
            PackageArtifactPage apage = asPage(PackageArtifactPage.class);
            if (hasNavigationHeader()) {
                apage.checkSelectedTab();
            }
            apage.checkReference(partial, legacy);
        }
    }

    protected void checkLiveJson(boolean check404) {
        openAndCheck(String.format("%s%s/", ExplorerHomePage.URL, "json"), check404);
        if (!check404) {
            String content = driver.findElementByTagName("pre").getText();
            assertNotNull(content);
            content = content.trim();
            assertTrue(content.startsWith("{") && content.endsWith("}"));
        }
    }

    protected String previousWindowHandle;

    protected void storeWindowHandle() {
        previousWindowHandle = driver.getWindowHandle();
    }

    protected void switchToNewWindow() {
        Wait<WebDriver> wait = Locator.getFluentWait();
        wait.until((new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    // wait for one more window to be opened
                    return driver.getWindowHandles().size() > 1;
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            }
        }));
        // select last handle
        String handle = null;
        for (String h : driver.getWindowHandles()) {
            handle = h;
        }
        // switch to it
        driver.switchTo().window(handle);
    }

    protected void switchBackToPreviousWindow() {
        if (previousWindowHandle != null) {
            driver.close();
            driver.switchTo().window(previousWindowHandle);
        }
    }

    /**
     * @since 11.2
     */
    public static String getReferenceContent(Path path) throws IOException {
        try (InputStream stream = getReferenceStream(path)) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8).trim();
        }
    }

    /**
     * @since 11.2
     */
    public static InputStream getReferenceStream(Path path) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path.toString());
    }

}

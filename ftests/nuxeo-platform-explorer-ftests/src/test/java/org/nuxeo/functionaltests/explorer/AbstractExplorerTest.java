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
package org.nuxeo.functionaltests.explorer;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.explorer.pages.ListingFragment;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ComponentArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ContributionArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ExtensionPointArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.OperationArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.ServiceArtifactPage;

/**
 * @since 11.1
 */
public abstract class AbstractExplorerTest extends AbstractTest {

    protected void doLogin() {
        getLoginPage().login(TEST_USERNAME, TEST_PASSWORD);
    }

    protected void doLogout() {
        // logout avoiding JS error check
        driver.get(NUXEO_URL + "/logout");
    }

    protected ExplorerHomePage goHome() {
        open(ExplorerHomePage.URL);
        return asPage(ExplorerHomePage.class);
    }

    protected boolean hasNavigationHeader() {
        return true;
    }

    protected void checkExtensionPoints() {
        ListingFragment listing = asPage(ListingFragment.class);
        listing.checkListing(-1, "actions", "/viewExtensionPoint/org.nuxeo.ecm.platform.actions.ActionService--actions",
                "ActionService - org.nuxeo.ecm.platform.actions.ActionService");

        listing = listing.filterOn("org.nuxeo.apidoc");
        listing.checkListing(1, "plugins",
                "/viewExtensionPoint/org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins",
                "SnapshotManagerComponent - org.nuxeo.apidoc.snapshot.SnapshotManagerComponent");

        listing.navigateToFirstItem();
        ExtensionPointArtifactPage apage = asPage(ExtensionPointArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.check();
    }

    protected void checkContributions() {
        ListingFragment listing = asPage(ListingFragment.class);
        listing.checkListing(-1, "cluster-config--configuration", "/viewContribution/cluster-config--configuration",
                "configuration - org.nuxeo.runtime.cluster.ClusterService");
        listing = listing.filterOn("org.nuxeo.apidoc");
        listing.checkListing(6, "org.nuxeo.apidoc.adapterContrib--adapters",
                "/viewContribution/org.nuxeo.apidoc.adapterContrib--adapters",
                "adapters - org.nuxeo.ecm.core.api.DocumentAdapterService");

        listing.navigateToFirstItem();
        ContributionArtifactPage apage = asPage(ContributionArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.check();
    }

    protected void checkServices() {
        ListingFragment listing = asPage(ListingFragment.class);
        listing.checkListing(-1, "ActionManager", "/viewService/org.nuxeo.ecm.platform.actions.ejb.ActionManager",
                "org.nuxeo.ecm.platform.actions.ejb.ActionManager");

        // toggle sort to check the SnapshotManager service
        listing = listing.filterOn("org.nuxeo.apidoc").toggleSort();
        listing.checkListing(3, "SnapshotManager", "/viewService/org.nuxeo.apidoc.snapshot.SnapshotManager",
                "org.nuxeo.apidoc.snapshot.SnapshotManager");

        listing.navigateToFirstItem();
        ServiceArtifactPage apage = asPage(ServiceArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.check();
    }

    protected void checkOperations() {
        ListingFragment listing = asPage(ListingFragment.class);
        listing.checkListing(-1, "acceptComment", "/viewOperation/acceptComment", "CHAIN acceptComment");

        listing = listing.filterOn("acceptComment");
        listing.checkListing(1, "acceptComment", "/viewOperation/acceptComment", "CHAIN acceptComment");

        listing.navigateToFirstItem();
        OperationArtifactPage apage = asPage(OperationArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.check();
    }

    protected void checkComponents() {
        ListingFragment listing = asPage(ListingFragment.class);
        listing.checkListing(-1, "actions.ActionService", "/viewComponent/org.nuxeo.ecm.platform.actions.ActionService",
                "JAVA org.nuxeo.ecm.platform.actions.ActionService");

        listing = listing.filterOn("org.nuxeo.apidoc");
        listing.checkListing(8, "apidoc.adapterContrib", "/viewComponent/org.nuxeo.apidoc.adapterContrib",
                "XML org.nuxeo.apidoc.adapterContrib");

        listing.navigateToFirstItem();
        ComponentArtifactPage apage = asPage(ComponentArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.check();
    }

    protected void checkBundles() {
        ListingFragment listing = asPage(ListingFragment.class);
        listing.checkListing(-1, "org.nuxeo.admin.center", "/viewBundle/org.nuxeo.admin.center", null);

        listing = listing.filterOn("org.nuxeo.apidoc");
        listing.checkListing(3, "org.nuxeo.apidoc.core", "/viewBundle/org.nuxeo.apidoc.core", null);

        listing.navigateToFirstItem();
        BundleArtifactPage apage = asPage(BundleArtifactPage.class);
        if (hasNavigationHeader()) {
            apage.checkSelectedTab();
        }
        apage.check();
    }

}

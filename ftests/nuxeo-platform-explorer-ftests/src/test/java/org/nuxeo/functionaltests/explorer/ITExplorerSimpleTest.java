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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.ListingFragment;
import org.nuxeo.functionaltests.explorer.pages.LiveSimplePage;
import org.nuxeo.functionaltests.explorer.pages.artifacts.BundleArtifactPage;

/**
 * Test explorer main webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerSimpleTest extends AbstractExplorerTest {

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

    @Override
    protected boolean hasNavigationHeader() {
        return false;
    }

    /**
     * Non-regression test for NXP-28911.
     */
    @Test
    public void testLiveDistributionSimplePage() {
        open(LiveSimplePage.URL);
        LiveSimplePage distrib = asPage(LiveSimplePage.class);
        distrib.check();

        ListingFragment listing = asPage(ListingFragment.class);
        listing.checkListing(-1, "org.nuxeo.admin.center", "/viewBundle/org.nuxeo.admin.center", null);
        listing = listing.filterOn("org.nuxeo.apidoc");
        listing.checkListing(3, "org.nuxeo.apidoc.core", "/viewBundle/org.nuxeo.apidoc.core", null);

        listing.navigateToFirstItem();
        asPage(BundleArtifactPage.class).check();

        open(LiveSimplePage.URL + "viewBundle/org.nuxeo.apidoc.core");
        asPage(BundleArtifactPage.class).check();
    }

    @Test
    public void testExtensionPoints() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_EXTENSIONPOINTS);
        checkExtensionPoints(false);
    }

    @Test
    public void testContributions() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_CONTRIBUTIONS);
        checkContributions(false);
    }

    @Test
    public void testServices() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_SERVICES);
        checkServices(false);
    }

    @Test
    public void testOperations() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_OPERATIONS);
        checkOperations(false);
    }

    @Test
    public void testComponents() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_COMPONENTS);
        checkComponents(false);
    }

    @Test
    public void testBundles() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_BUNDLES);
        checkBundles(false);
    }

    @Test
    public void testBundlesGroups() {
        open(LiveSimplePage.URL + ApiBrowserConstants.LIST_BUNDLEGROUPS);
        checkBundleGroups();
    }

}

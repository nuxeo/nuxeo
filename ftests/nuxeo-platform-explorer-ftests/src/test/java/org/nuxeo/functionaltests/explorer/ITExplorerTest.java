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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.explorer.pages.ArtifactHomePage;
import org.nuxeo.functionaltests.explorer.pages.ArtifactPage;
import org.nuxeo.functionaltests.explorer.pages.ExplorerHomePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test explorer main webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerTest extends AbstractTest {

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

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() throws UserNotConnectedException {
        goHome();
    }

    @Test
    public void testHomePage() throws UserNotConnectedException {
        ExplorerHomePage home = goHome();
        assertEquals("Nuxeo Platform Explorer", home.getTitle());
        assertEquals("Running Platform".toUpperCase(), home.currentPlatform.getText());

        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        assertEquals("All Extension Points", ahome.getTitle());
        ahome = goHome().navigateTo(home.currentContributions);
        assertEquals("All Contributions", ahome.getTitle());
        ahome = goHome().navigateTo(home.currentOperations);
        assertEquals("All Operations", ahome.getTitle());
        ahome = goHome().navigateTo(home.currentServices);
        assertEquals("All Services", ahome.getTitle());
    }

    @Test
    public void testExtensionPoints() throws UserNotConnectedException {
        ExplorerHomePage home = goHome();
        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        assertTrue(ahome.isSelected(ahome.extensionPoints));
        WebElement elt = ahome.getFirstListingElement();
        WebElement link = elt.findElement(By.xpath(".//a"));
        assertEquals("actions", link.getText());
        assertEquals("ActionService", elt.findElement(By.xpath(".//span[@title='Component Label']")).getText());
        assertEquals("org.nuxeo.ecm.platform.actions.ActionService",
                elt.findElement(By.xpath(".//span[@title='Component ID']")).getText());
        Locator.scrollAndForceClick(link);
        ArtifactPage apage = asPage(ArtifactPage.class);
        assertTrue(apage.isSelected(apage.extensionPoints));
        assertEquals("Extension point org.nuxeo.ecm.platform.actions.ActionService--actions", apage.getTitle());
        assertEquals("Extension point actions", apage.header.getText());
        assertEquals("In component org.nuxeo.ecm.platform.actions.ActionService", apage.description.getText());
    }

    @Test
    public void testContributions() throws UserNotConnectedException {
        ExplorerHomePage home = goHome();
        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        ahome = ahome.navigateTo(ahome.contributions);
        assertTrue(ahome.isSelected(ahome.contributions));
        WebElement elt = ahome.getFirstListingElement();
        WebElement link = elt.findElement(By.xpath(".//a"));
        assertEquals("cluster-config--configuration", link.getText());
        assertEquals("configuration", elt.findElement(By.xpath(".//span[@title='Target Extension Point']")).getText());
        assertEquals("org.nuxeo.runtime.cluster.ClusterService",
                elt.findElement(By.xpath(".//span[@title='Target Component Name']")).getText());
        Locator.scrollAndForceClick(link);
        ArtifactPage apage = asPage(ArtifactPage.class);
        assertTrue(apage.isSelected(apage.contributions));
        assertEquals("Contribution cluster-config--configuration", apage.getTitle());
        assertEquals("Contribution cluster-config--configuration", apage.header.getText());
    }

    @Test
    public void testServices() throws UserNotConnectedException {
        ExplorerHomePage home = goHome();
        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        ahome = ahome.navigateTo(ahome.services);
        assertTrue(ahome.isSelected(ahome.services));
        WebElement elt = ahome.getFirstListingElement();
        WebElement link = elt.findElement(By.xpath(".//a"));
        assertEquals("ActionManager", link.getText());
        assertEquals("org.nuxeo.ecm.platform.actions.ejb.ActionManager",
                elt.findElement(By.xpath(".//span[@title='Service Name']")).getText());
        Locator.scrollAndForceClick(link);
        ArtifactPage apage = asPage(ArtifactPage.class);
        assertTrue(apage.isSelected(apage.services));
        assertEquals("Service org.nuxeo.ecm.platform.actions.ejb.ActionManager", apage.getTitle());
        assertEquals("Service org.nuxeo.ecm.platform.actions.ejb.ActionManager", apage.header.getText());
        assertEquals("In component org.nuxeo.ecm.platform.actions.ActionService", apage.description.getText());
    }

    @Test
    public void testOperations() throws UserNotConnectedException {
        ExplorerHomePage home = goHome();
        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        ahome = ahome.navigateTo(ahome.operations);
        assertTrue(ahome.isSelected(ahome.operations));
        WebElement elt = ahome.getFirstListingElement();
        WebElement link = elt.findElement(By.xpath(".//a"));
        assertEquals("acceptComment", link.getText());
        assertEquals("CHAIN", elt.findElement(By.xpath(".//span[@title='Category']")).getText());
        assertEquals("acceptComment", elt.findElement(By.xpath(".//span[@title='Operation ID']")).getText());
        Locator.scrollAndForceClick(link);
        ArtifactPage apage = asPage(ArtifactPage.class);
        assertTrue(apage.isSelected(apage.operations));
        assertEquals("Operation acceptComment", apage.getTitle());
        assertEquals("Operation acceptComment", apage.header.getText());
        assertEquals("acceptComment", apage.description.getText());
    }

    @Test
    public void testComponents() throws UserNotConnectedException {
        ExplorerHomePage home = goHome();
        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        ahome = ahome.navigateTo(ahome.components);
        assertTrue(ahome.isSelected(ahome.components));
        WebElement elt = ahome.getFirstListingElement();
        WebElement link = elt.findElement(By.xpath(".//a"));
        assertEquals("actions.ActionService", link.getText());
        assertEquals("JAVA", elt.findElement(By.xpath(".//span[@title='Component Type']")).getText());
        assertEquals("org.nuxeo.ecm.platform.actions.ActionService",
                elt.findElement(By.xpath(".//span[@title='Component ID']")).getText());
        Locator.scrollAndForceClick(link);
        ArtifactPage apage = asPage(ArtifactPage.class);
        assertTrue(apage.isSelected(apage.components));
        assertEquals("Component org.nuxeo.ecm.platform.actions.ActionService", apage.getTitle());
        assertEquals("Component org.nuxeo.ecm.platform.actions.ActionService", apage.header.getText());
        assertEquals("In bundle org.nuxeo.ecm.actions", apage.description.getText());
    }

    @Test
    public void testBundles() throws UserNotConnectedException {
        ExplorerHomePage home = goHome();
        ArtifactHomePage ahome = home.navigateTo(home.currentExtensionPoints);
        ahome = ahome.navigateTo(ahome.bundles);
        assertTrue(ahome.isSelected(ahome.bundles));
        WebElement elt = ahome.getFirstListingElement();
        WebElement link = elt.findElement(By.xpath(".//a"));
        assertEquals("org.nuxeo.admin.center", link.getText());
        Locator.scrollAndForceClick(link);
        ArtifactPage apage = asPage(ArtifactPage.class);
        assertTrue(apage.isSelected(apage.bundles));
        assertEquals("Bundle org.nuxeo.admin.center", apage.getTitle());
        assertEquals("Bundle org.nuxeo.admin.center", apage.header.getText());
    }

}

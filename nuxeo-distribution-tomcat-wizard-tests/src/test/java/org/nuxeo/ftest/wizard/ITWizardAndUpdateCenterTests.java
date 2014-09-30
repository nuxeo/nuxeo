/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 */

package org.nuxeo.ftest.wizard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.nuxeo.functionaltests.pages.admincenter.ConnectHomePage;
import org.nuxeo.functionaltests.pages.admincenter.PackageInstallationScreen;
import org.nuxeo.functionaltests.pages.admincenter.PackageListingPage;
import org.nuxeo.functionaltests.pages.admincenter.SystemHomePage;
import org.nuxeo.functionaltests.pages.admincenter.UpdateCenterPage;
import org.nuxeo.functionaltests.pages.wizard.ConnectRegistrationPage;
import org.nuxeo.functionaltests.pages.wizard.ConnectWizardPage;
import org.nuxeo.functionaltests.pages.wizard.IFrameHelper;
import org.nuxeo.functionaltests.pages.wizard.SummaryWizardPage;
import org.nuxeo.functionaltests.pages.wizard.WizardPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ITWizardAndUpdateCenterTests extends AbstractTest {

    private static final String MARKETPLACE_PACKAGE_ID = "audit-web-access-1.0.6";

    private static final String STUDIO_PACKAGE_ID_FIRST = "junit4tester-SANDBOX-0.0.1";

    private static final String STUDIO_PACKAGE_ID_SECOND = "junit4tester-SANDBOX-0.0.2";

    private static final String SMTP_SERVER_HOST = "someSMTPServer.com";

    private static final String SMTP_SERVER_PORT = "27";

    protected static final String NX_LOGIN = "Administrator";

    protected static final String NX_PASSWORD = "Administrator";

    protected static final String CONNECT_LOGIN = "junit4tester";

    protected static final String CONNECT_PROJECT_SELECTOR = "junit4tester";

    protected static final String CONNECT_PROJECT_SELECTOR_UUID = "575954be-6027-45b7-8cd1-77a6bcb0832d";

    public static final String CONNECT_FORM_TITLE = "Nuxeo Connect & Nuxeo Studio";

    @Test
    public void testAll() throws Exception {
        runWizardAndRestart();
        installPackageAndRestart();
        verifyPackageInstallation();
        studioPackageInstallAndUninstall();
        verifyPackageUndeployProcessUnderWindows();
    }

    protected String getTestPassword() {
        return  System.getProperty("connectPassword");
    }

    public void runWizardAndRestart() throws Exception {
        // **********************
        // welcome
        WizardPage welcomePage = get(NUXEO_URL, WizardPage.class);
        assertTrue(welcomePage.getTitle().contains("Welcome to "));

        // **********************
        // Settings
        WizardPage settingsPage = welcomePage.next();
        assertNotNull(settingsPage);

        assertEquals("General Settings", settingsPage.getTitle());

        welcomePage = settingsPage.previous();
        assertNotNull(welcomePage);
        assertFalse(welcomePage.hasError());
        assertTrue(welcomePage.getTitle().contains("Welcome to "));

        // **********************
        // proxy
        WizardPage proxyPage = welcomePage.next().next();
        assertNotNull(proxyPage);
        assertFalse(proxyPage.hasError());
        assertEquals("HTTP Proxy Settings", proxyPage.getTitle());

        // check validation
        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type", "anonymous"));
        proxyPage.clearInput("nuxeo.http.proxy.host");
        proxyPage.clearInput("nuxeo.http.proxy.port");
        proxyPage = proxyPage.next(true);
        assertTrue(proxyPage.hasError());

        proxyPage.fillInput("nuxeo.http.proxy.host", "myproxy");
        proxyPage.fillInput("nuxeo.http.proxy.port", "AAAA");
        proxyPage = proxyPage.next(true);
        assertTrue(proxyPage.hasError());

        proxyPage.fillInput("nuxeo.http.proxy.port", "8080");
        WizardPage somePage = proxyPage.next();
        assertFalse(somePage.hasError());
        proxyPage = somePage.previous();

        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type",
                "authenticated"));
        proxyPage.clearInput("nuxeo.http.proxy.login");
        proxyPage.clearInput("nuxeo.http.proxy.password");
        proxyPage = proxyPage.next(true);
        assertTrue(proxyPage.hasError());

        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type", "none"));

        // **********************
        // Database settings
        WizardPage dbPage = proxyPage.next();
        assertNotNull(dbPage);
        assertFalse(dbPage.hasError());
        assertEquals("Database Settings", dbPage.getTitle());

        // **********************
        // SMTP Settings
        WizardPage smtpPage = dbPage.next();
        assertNotNull(smtpPage);
        assertEquals("SMTP Settings", smtpPage.getTitle());
        // check port validation
        assertTrue(smtpPage.selectOption("mail.transport.auth", "false"));
        smtpPage.fillInput("mail.transport.host", SMTP_SERVER_HOST);
        smtpPage.fillInput("mail.transport.port", "AAA");
        smtpPage = smtpPage.next(true);
        assertTrue(smtpPage.hasError());

        // check login/password validation
        smtpPage.fillInput("mail.transport.port", SMTP_SERVER_PORT);
        assertTrue(smtpPage.selectOption("mail.transport.auth", "true"));
        smtpPage.clearInput("mail.transport.user");
        smtpPage.clearInput("mail.transport.password");
        smtpPage = smtpPage.next(true);
        assertTrue(smtpPage.hasError());
        assertTrue(smtpPage.selectOption("mail.transport.auth", "false"));

        // **********************
        // Connect Form
        WizardPage connectWizardPage = smtpPage.next();
        assertNotNull(connectWizardPage);
        assertFalse(connectWizardPage.hasError());

        // enter embedded IFrame
        ConnectWizardPage connectPage1 = connectWizardPage.getConnectPage();
        assertNotNull(connectPage1);
        assertEquals(CONNECT_FORM_TITLE, connectPage1.getTitle());

        // try to validate
        ConnectWizardPage connectPage2 = connectPage1.submitWithError();
        assertNotNull(connectPage2);
        assertTrue(connectPage2.getErrorMessage().startsWith(
                "There were some errors in your form:"));

        // ok, let's try to skip the screen
        WizardPage connectSkip = connectPage1.navByLink(WizardPage.class,
                "Or skip and don't register", true);
        assertNotNull(connectSkip);
        assertEquals("You have not signed up for a free trial of Nuxeo Connect.",
                connectSkip.getTitle2());

        // ok, let's register
        connectWizardPage = connectSkip.navById(WizardPage.class, "btnRetry",
                true);
        connectPage1 = connectWizardPage.getConnectPage(); // enter iframe again
        assertNotNull(connectPage1);

        // Register with a existing account
        ConnectRegistrationPage connectSignIn = connectPage1.getLink(
                "click here").asPage(ConnectRegistrationPage.class);

        // Login through CAS
        String mainWindow = driver.getWindowHandle();
        WebDriver popup = AbstractTest.getPopup();
        System.out.println(popup.getCurrentUrl());
        popup.findElement(By.id("username")).sendKeys(CONNECT_LOGIN);
        popup.findElement(By.id("password")).sendKeys(getTestPassword());
        popup.findElement(By.cssSelector(".btn-submit")).click();

        driver.switchTo().window(mainWindow);

        IFrameHelper.focusOnConnectFrame(driver);
        assertEquals("Register your new Nuxeo instance",
                connectSignIn.getTitle());

        // select the associated project
        connectSignIn.selectOption("project",
                CONNECT_PROJECT_SELECTOR_UUID);
        // connectProjectPage.fillInput("project", CONNECT_PROJECT_SELECTOR);

        // **********************
        // Exit Connect Form and Display Packages selection
        WizardPage packageSelectiondPage = connectSignIn.nav(
                WizardPage.class, "Continue");

        assertNotNull(packageSelectiondPage);
        assertEquals("Select Modules", packageSelectiondPage.getTitle());

        // use specific url
        String currentUrl = driver.getCurrentUrl();
        currentUrl = currentUrl + "?showPresets=true";
        packageSelectiondPage = get(currentUrl, WizardPage.class);

        WebElement presetBtn = Locator.findElementWithTimeout(By.id("preset_nuxeo-dm"));
        presetBtn.click();
        Thread.sleep(1000);

        // **************************
        // Package Download Screen
        WizardPage packageDownloadPage = packageSelectiondPage.next();
        assertNotNull(packageDownloadPage);
        assertEquals("Download Module(s)", packageDownloadPage.getTitle());

        // **********************
        // Summary screen
        SummaryWizardPage summary = packageDownloadPage.next(SummaryWizardPage.class);
        assertNotNull(summary);
        assertEquals("Summary", summary.getTitle());
        assertNotNull(summary.getRegistration());

        // Restart
        summary.restart();
    }

    public void loopOnIframe() throws Exception {

        // **********************
        // welcome
        WizardPage welcomePage = get(NUXEO_URL, WizardPage.class);
        assertTrue(welcomePage.getTitle().contains("Welcome to "));

        // **********************
        // Settings
        WizardPage settingsPage = welcomePage.next();
        assertNotNull(settingsPage);

        assertEquals("General Settings", settingsPage.getTitle());

        // **********************
        // proxy
        WizardPage proxyPage = settingsPage.next();
        assertNotNull(proxyPage);
        assertFalse(proxyPage.hasError());
        assertEquals("HTTP Proxy Settings", proxyPage.getTitle());

        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type", "none"));

        // **********************
        // Database settings
        WizardPage dbPage = proxyPage.next();
        assertNotNull(dbPage);
        assertFalse(dbPage.hasError());
        assertEquals("Database Settings", dbPage.getTitle());

        // **********************
        // SMTP Settings
        WizardPage smtpPage = dbPage.next();
        assertNotNull(smtpPage);
        assertEquals("SMTP Settings", smtpPage.getTitle());
        assertTrue(smtpPage.selectOption("mail.transport.auth", "false"));

        // **********************
        // Connect Form

        WizardPage connectWizardPage = smtpPage.next(WizardPage.class);

        for (int i = 1; i < 20; i++) {

            assertNotNull(connectWizardPage);
            assertFalse(connectWizardPage.hasError());

            // enter embedded IFrame
            System.out.println(driver.getCurrentUrl());
            ConnectWizardPage connectPage1 = connectWizardPage.getConnectPage();
            System.out.println(driver.getCurrentUrl());
            assertNotNull(connectPage1);
            assertEquals(CONNECT_FORM_TITLE, connectPage1.getTitle());

            // try to validate
            ConnectWizardPage connectPage2 = connectPage1.next(ConnectWizardPage.class);
            assertNotNull(connectPage2);
            assertEquals(
                    "There were some errors in your form: You must define a login",
                    connectPage2.getErrorMessage());

            // ok, let's try to skip the screen
            ConnectWizardPage connectSkip = connectPage1.nav(
                    ConnectWizardPage.class, "Skip");
            assertNotNull(connectSkip);
            assertEquals(
                    "You have not registered your instance on Nuxeo Connect.",
                    connectSkip.getTitle2());

            // ok, let's register
            connectWizardPage = connectSkip.navById(WizardPage.class,
                    "btnRetry");

        }

    }

    @Test
    @Ignore
    public void testRestartFromAdminCenter() throws UserNotConnectedException {
        // login
        DocumentBasePage home = login(NX_LOGIN, NX_PASSWORD);
        // Open Admin Center and restart
        AdminCenterBasePage adminHome = home.getAdminCenter();
        assertNotNull(adminHome);
        SystemHomePage systemHome = adminHome.getSystemHomePage();
        assertNotNull(systemHome);
        systemHome.restart();
    }

    public void installPackageAndRestart() throws Exception {
        // login
        DocumentBasePage home = login(NX_LOGIN, NX_PASSWORD);

        // Open Admin Center
        AdminCenterBasePage adminHome = home.getAdminCenter();
        assertNotNull(adminHome);

        // Check registration on connect Home
        ConnectHomePage connectHome = adminHome.getConnectHomePage();
        assertNotNull(connectHome);
        assertEquals("Connect registration OK", connectHome.getConnectStatus());

        // Check setup parameters
        SystemHomePage systemPage = connectHome.getSystemHomePage();
        String smtpHost = systemPage.getConfig("mail.transport.host");
        assertEquals(SMTP_SERVER_HOST, smtpHost);

        // Go to Update Center
        UpdateCenterPage updateCenterHome = systemPage.getUpdateCenterHomePage();
        updateCenterHome = updateCenterHome.getPackagesFromNuxeoMarketPlace();

        // ensure there is no filter
        updateCenterHome.removePlatformFilterOnMarketPlacePage();

        // Get listing in IFrame
        PackageListingPage packageListing = updateCenterHome.getPackageListingPage();

        // Download Package
        WebElement link = packageListing.download(MARKETPLACE_PACKAGE_ID);
        assertNotNull(link);

        // Start installation
        PackageInstallationScreen installScreen = packageListing.getInstallationScreen(MARKETPLACE_PACKAGE_ID);
        assertNotNull(installScreen);

        packageListing = installScreen.start();
        assertNotNull(packageListing);
        WebElement packageLink = packageListing.getPackageLink(MARKETPLACE_PACKAGE_ID);
        assertNotNull(packageLink);
        assertTrue(packageLink.getText().trim().toLowerCase().startsWith(
                "restart"));

        updateCenterHome = packageListing.exit();
        assertNotNull(updateCenterHome);

        SystemHomePage systemHome = updateCenterHome.getSystemHomePage();
        assertNotNull(systemHome);

        systemHome.restart();
    }

    public void verifyPackageInstallation() throws Exception {
        DocumentBasePage home = login(NX_LOGIN, NX_PASSWORD);
        AdminCenterBasePage adminHome = home.getAdminCenter();
        assertNotNull(adminHome);

        SystemHomePage systemHomePage = adminHome.getSystemHomePage();
        systemHomePage.selectSubTab("Nuxeo distribution");

        WebElement bundle = Locator.findElementWithTimeout(By.xpath("//td[text()[normalize-space()='org.nuxeo.ecm.platform.audit.web.access']]"));
        assertNotNull(bundle);

        // Need to make HeaderLinksSubPage#logout work and use it
        navToUrl("http://localhost:8080/nuxeo/logout");
    }

    public void studioPackageInstallAndUninstall() throws Exception {
        // Login
        DocumentBasePage home = login(NX_LOGIN, NX_PASSWORD);
        AdminCenterBasePage adminHome = home.getAdminCenter();
        assertNotNull(adminHome);

        // Go to Update Center
        ConnectHomePage connectHome = adminHome.getConnectHomePage();
        assertNotNull(connectHome);
        assertEquals("Connect registration OK", connectHome.getConnectStatus());
        SystemHomePage systemPage = connectHome.getSystemHomePage();
        UpdateCenterPage updateCenterHome = systemPage.getUpdateCenterHomePage();
        updateCenterHome = updateCenterHome.getPackagesFromNuxeoMarketPlace();

        // Go to Studio Packages
        updateCenterHome = updateCenterHome.getPackagesFromNuxeoStudio();

        // Get listing in IFrame
        PackageListingPage packageListing = updateCenterHome.getPackageListingPage();

        // Download Packages
        WebElement link = packageListing.download(STUDIO_PACKAGE_ID_FIRST);
        assertNotNull(link);
        link = packageListing.download(STUDIO_PACKAGE_ID_SECOND);
        assertNotNull(link);

        // Start first bundle installation
        PackageInstallationScreen installScreen = packageListing.getInstallationScreen(STUDIO_PACKAGE_ID_FIRST);
        assertNotNull(installScreen);
        packageListing = installScreen.start();
        assertNotNull(packageListing);
        WebElement packageLink = packageListing.getPackageLink(STUDIO_PACKAGE_ID_FIRST);
        assertNotNull(packageLink);

        // Start second bundle installation
        installScreen = packageListing.getInstallationScreen(STUDIO_PACKAGE_ID_SECOND);
        assertNotNull(installScreen);
        packageListing = installScreen.start();
        assertNotNull(packageListing);
        packageLink = packageListing.getPackageLink(STUDIO_PACKAGE_ID_SECOND);
        assertNotNull(packageLink);

        // Restart application for uninstall test for windows
        updateCenterHome = packageListing.exit();
        SystemHomePage systemHome = updateCenterHome.getSystemHomePage();
        systemHome.restart();
    }

    public void verifyPackageUndeployProcessUnderWindows() throws Exception {
        // Login
        DocumentBasePage home = login(NX_LOGIN, NX_PASSWORD);
        AdminCenterBasePage adminHome = home.getAdminCenter();
        assertNotNull(adminHome);

        // Go to Update Center
        ConnectHomePage connectHome = adminHome.getConnectHomePage();
        assertNotNull(connectHome);
        assertEquals("Connect registration OK", connectHome.getConnectStatus());
        SystemHomePage systemPage = connectHome.getSystemHomePage();
        UpdateCenterPage updateCenterHome = systemPage.getUpdateCenterHomePage();
        updateCenterHome = updateCenterHome.getPackagesFromNuxeoMarketPlace();

        // Go to Studio Packages
        updateCenterHome = updateCenterHome.getPackagesFromNuxeoStudio();

        // Get listing in IFrame
        PackageListingPage packageListing = updateCenterHome.getPackageListingPage();

        // Reinstall first package for undeploying the second
        PackageInstallationScreen installScreen = packageListing.getInstallationScreen(STUDIO_PACKAGE_ID_FIRST);
        assertNotNull(installScreen);
        packageListing = installScreen.start();
        assertNotNull(packageListing);
        WebElement packageLink = packageListing.getPackageLink(STUDIO_PACKAGE_ID_FIRST);
        assertNotNull(packageLink);

        // Need to make HeaderLinksSubPage#logout work and use it
        navToUrl("http://localhost:8080/nuxeo/logout");
    }
}

/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */

package org.nuxeo.ftest.wizard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.JavaScriptErrorCollector.JavaScriptErrorIgnoreRule;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.nuxeo.functionaltests.pages.admincenter.ConnectHomePage;
import org.nuxeo.functionaltests.pages.admincenter.PackageInstallationScreen;
import org.nuxeo.functionaltests.pages.admincenter.PackageListingPage;
import org.nuxeo.functionaltests.pages.admincenter.SystemHomePage;
import org.nuxeo.functionaltests.pages.admincenter.UpdateCenterPage;
import org.nuxeo.functionaltests.pages.wizard.ConnectRegistrationPage;
import org.nuxeo.functionaltests.pages.wizard.ConnectWizardPage;
import org.nuxeo.functionaltests.pages.wizard.SummaryWizardPage;
import org.nuxeo.functionaltests.pages.wizard.WizardPage;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

public class ITWizardAndUpdateCenterTests extends AbstractTest {

    private static final Log log = LogFactory.getLog(ITWizardAndUpdateCenterTests.class);

    protected static final String MARKETPLACE_PACKAGE_ID = "audit-web-access-1.0.6";

    protected static final String STUDIO_PACKAGE_ID_FIRST = "junit4tester-SANDBOX-0.0.1";

    protected static final String STUDIO_PACKAGE_ID_SECOND = "junit4tester-SANDBOX-0.0.2";

    protected static final String SMTP_SERVER_HOST = "someSMTPServer.com";

    protected static final String SMTP_SERVER_PORT = "27";

    protected static final String NX_LOGIN = "Administrator";

    protected static final String NX_PASSWORD = "Administrator";

    protected static final String CONNECT_LOGIN = "junit4tester";

    protected static final String CONNECT_PROJECT_SELECTOR_UUID = "575954be-6027-45b7-8cd1-77a6bcb0832d";

    protected static final String CONNECT_FORM_TITLE = "Nuxeo Online Services";

    protected static final String REGISTRATION_OK = "Nuxeo Online Services registration is OK.";

    protected String getTestPassword() {
        return System.getProperty("connectPassword");
    }

    @Test
    public void runWizardAndRestart() throws Exception {
        addAfterTestIgnores(JavaScriptErrorIgnoreRule.fromSource("https://fast.wistia.com"));
        addAfterTestIgnores(JavaScriptErrorIgnoreRule.fromSource("https://js.intercomcdn.com"));
        addAfterTestIgnores(JavaScriptErrorIgnoreRule.fromSource("https://www.nuxeo.com/standalone-login-page"));

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

        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type", "authenticated"));
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
        // Directory settings
        WizardPage userPage = dbPage.next();
        assertNotNull(userPage);
        assertFalse(userPage.hasError());
        assertEquals("Users & Groups Settings", userPage.getTitle());
        userPage.selectOptionWithReload("nuxeo.directory.type", "ldap");
        userPage.fillInput("nuxeo.ldap.url", "ldap://ldap.testathon.net:3890");
        userPage = userPage.navById(WizardPage.class, "checkNetwork");
        assertTrue(userPage.hasError());
        userPage.clearInput("nuxeo.ldap.url");
        userPage.fillInput("nuxeo.ldap.url", "ldaps://ldap-test.nuxeo.com:636");
        userPage = userPage.navById(WizardPage.class, "checkNetwork");
        assertFalse(userPage.hasError());
        userPage.selectOptionWithReload("nuxeo.directory.type", "default");

        // **********************
        // SMTP Settings
        WizardPage smtpPage = userPage.next();
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
        smtpPage.next();

        registerConnectInstance();

        // **********************
        // Online Registration exited and Display Packages page must be visible
        WizardPage packageSelectiondPage = asPage(WizardPage.class);

        assertNotNull(packageSelectiondPage);
        assertEquals("Select Addons", packageSelectiondPage.getTitle());

        AbstractPage.findElementWithTimeout(By.id("pkg_nuxeo-no-ui"));
        AbstractPage.findElementWithTimeout(By.id("pkg_nuxeo-web-ui"));

        AbstractPage.findElementWaitUntilEnabledAndClick(By.id("pkg_nuxeo-showcase-content"));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.id("pkg_nuxeo-template-rendering-samples"));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.id("pkg_nuxeo-template-rendering"));

        // **************************
        // Package Download Screen
        WizardPage packageDownloadPage = packageSelectiondPage.next();
        assertNotNull(packageDownloadPage);
        assertEquals("Download Addon(s)", packageDownloadPage.getTitle());

        // **********************
        // Summary screen
        SummaryWizardPage summary = packageDownloadPage.next(SummaryWizardPage.class);
        assertNotNull(summary);
        assertEquals("Summary", summary.getTitle());
        assertNotNull(summary.getRegistration());

        // Restart
        summary.restart();
    }

    public void registerConnectInstance() throws InterruptedException {
        // **********************
        // Connect Form
        WizardPage connectWizardPage = asPage(WizardPage.class);
        // No need to timeout a lot, page is already loaded due to previous timeout.
        if (!Locator.hasElementWithTimeout(By.linkText("Register Your Instance"), 200)) {
            // Instance already registered
            connectWizardPage.next();
            return;
        }

        // Save main window handle to ease the switch back
        String mainWindowsHandle = driver.getWindowHandle();

        // enter on popup
        ConnectWizardPage connectPage1 = connectWizardPage.getConnectPage();
        assertNotNull(connectPage1);
        assertEquals(CONNECT_FORM_TITLE, connectPage1.getTitle());

        // try to validate
        ConnectWizardPage connectPage2 = connectPage1.submitWithError();
        assertNotNull(connectPage2);
        assertTrue(connectPage2.getErrorMessage().startsWith("There were some errors in your form:"));

        // Register with a existing account
        ConnectRegistrationPage connectSignIn = connectPage1.openLink("click here")
                                                            .asPage(ConnectRegistrationPage.class);

        // Login through CAS
        AbstractTest.switchToPopup("sso");
        driver.findElement(By.id("username")).sendKeys(CONNECT_LOGIN);
        driver.findElement(By.id("password")).sendKeys(getTestPassword());
        String windowHandle = driver.getWindowHandle();

        Locator.findElementWithTimeoutAndClick(By.cssSelector(".btn-submit"));
        try {
            Locator.waitUntilWindowClosed(windowHandle);
        } catch (TimeoutException e) {
            log.info("Unable to close SSO form once; try again");
            Locator.findElementWithTimeoutAndClick(By.cssSelector(".btn-submit"));
            Locator.waitUntilWindowClosed(windowHandle);
        }

        // select the associated project
        switchToPopup("site/connect/wizardInstanceRegistration");

        connectSignIn.selectOption("project", CONNECT_PROJECT_SELECTOR_UUID);
        connectSignIn.submit();

        driver.switchTo().window(mainWindowsHandle);
        Thread.sleep(1000);

        // After form submit; redirect is automatic and can be already done.
        String currentUrl = driver.getCurrentUrl();
        if (!currentUrl.contains("PackagesSelection")) {
            Locator.waitUntilURLDifferentFrom(currentUrl);
        }
    }

    @Test
    @Ignore
    public void testAdminCenter() throws Exception {
        studioPackageInstallAndUninstall();
        installPackageAndRestart();
        verifyPackageInstallation();
        verifyPackageUndeployProcessUnderWindows();
    }

    @Test
    @Ignore
    public void testRestartFromAdminCenter() throws Exception {
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

        // Go to Update Center
        UpdateCenterPage updateCenterHome = adminHome.getUpdateCenterHomePage();
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
        assertTrue(packageLink.getText().trim().toLowerCase().startsWith("restart"));

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

        WebElement bundle = Locator.findElementWithTimeout(
                By.xpath("//td[text()[normalize-space()='org.nuxeo.ecm.platform.audit.web.access']]"));
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
        assertEquals(REGISTRATION_OK, connectHome.getConnectStatus());
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
        assertEquals(REGISTRATION_OK, connectHome.getConnectStatus());
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

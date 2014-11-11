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

package org.nuxeo.functionaltests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.nuxeo.functionaltests.pages.admincenter.ConnectHomePage;
import org.nuxeo.functionaltests.pages.admincenter.PackageInstallationScreen;
import org.nuxeo.functionaltests.pages.admincenter.PackageListingPage;
import org.nuxeo.functionaltests.pages.admincenter.SystemHomePage;
import org.nuxeo.functionaltests.pages.admincenter.UpdateCenterPage;
import org.nuxeo.functionaltests.pages.wizard.ConnectWizardPage;
import org.nuxeo.functionaltests.pages.wizard.SummaryWizardPage;
import org.nuxeo.functionaltests.pages.wizard.WizardPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ITWizardAndUpdateCenter extends AbstractTest {

    private static final String MARKETPLACE_PACKAGE_ID = "audit-web-access-1.0.4";

    private static final String SMTP_SERVER_HOST = "someSMTPServer.com";

    private static final String SMTP_SERVER_PORT = "27";

    protected static final String NX_LOGIN = "Administrator";

    protected static final String NX_PASSWORD = "Administrator";

    protected static final String CONNECT_LOGIN = "junit4tester";

    protected static final String CONNECT_PROJECT_SELECTOR = "junit4tester";
    protected static final String CONNECT_PROJECT_SELECTOR_UUID = "575954be-6027-45b7-8cd1-77a6bcb0832d";

    public static final String CONNECT_FORM_TITLE = "Enable Nuxeo Connect & Nuxeo Studio for your installation";

    protected String getTestPassword() {
        return "XXX";
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    //@Test
    public void runWizardAndRestart() throws Exception {

        // **********************
        // welcome
        WizardPage welcomePage = get(NUXEO_URL, WizardPage.class);
        assertTrue(welcomePage.getTitle().contains("Welcome to "));

        // **********************
        // Settings
        WizardPage settingsPage = welcomePage.next(true);
        assertNotNull(settingsPage);

        assertEquals("General settings", settingsPage.getTitle());

        welcomePage = settingsPage.previous(true);
        assertNotNull(welcomePage);
        assertFalse(welcomePage.hasError());
        assertTrue(welcomePage.getTitle().contains("Welcome to "));

        // **********************
        // proxy
        WizardPage proxyPage = welcomePage.next(true).next(true);
        assertNotNull(proxyPage);
        assertFalse(proxyPage.hasError());
        assertEquals("HTTP proxy settings", proxyPage.getTitle());

        // check validation
        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type", "anonymous"));
        proxyPage.clearInput("nuxeo.http.proxy.host");
        proxyPage.clearInput("nuxeo.http.proxy.port");
        proxyPage = proxyPage.next(false);
        assertTrue(proxyPage.hasError());

        proxyPage.fillInput("nuxeo.http.proxy.host", "myproxy");
        proxyPage.fillInput("nuxeo.http.proxy.port", "AAAA");
        proxyPage = proxyPage.next(false);
        assertTrue(proxyPage.hasError());

        proxyPage.fillInput("nuxeo.http.proxy.port", "8080");
        WizardPage somePage = proxyPage.next(true);
        assertFalse(somePage.hasError());
        proxyPage = somePage.previous(true);

        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type","authenticated"));
        proxyPage.clearInput("nuxeo.http.proxy.login");
        proxyPage.clearInput("nuxeo.http.proxy.password");
        proxyPage =proxyPage.next();
        assertTrue(proxyPage.hasError());

        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type", "none"));

        // **********************
        // Database settings
        WizardPage dbPage = proxyPage.next(true);
        assertNotNull(dbPage);
        assertFalse(dbPage.hasError());
        assertEquals("Database settings", dbPage.getTitle());

        // **********************
        // SMTP Settings
        WizardPage smtpPage = dbPage.next(true);
        assertNotNull(smtpPage);
        assertEquals("SMTP Settings", smtpPage.getTitle());
        // check port validation
        assertTrue(smtpPage.selectOption("mail.smtp.auth", "false"));
        smtpPage.fillInput("mail.smtp.host", SMTP_SERVER_HOST);
        smtpPage.fillInput("mail.smtp.port", "AAA");
        smtpPage = smtpPage.next(WizardPage.class, false);
        assertTrue(smtpPage.hasError());

        // check login/password validation
        smtpPage.fillInput("mail.smtp.port", SMTP_SERVER_PORT);
        assertTrue(smtpPage.selectOption("mail.smtp.auth", "true"));
        smtpPage.clearInput("mail.smtp.username");
        smtpPage.clearInput("mail.smtp.password");
        smtpPage = smtpPage.next(WizardPage.class, false);
        assertTrue(smtpPage.hasError());
        assertTrue(smtpPage.selectOption("mail.smtp.auth", "false"));

        // **********************
        // Connect Form
        WizardPage connectWizardPage = smtpPage.next(WizardPage.class, true);
        assertNotNull(connectWizardPage);
        assertFalse(connectWizardPage.hasError());

        // enter embedded IFrame
        System.out.println(driver.getCurrentUrl());
        ConnectWizardPage connectPage1 = connectWizardPage.getConnectPage();
        System.out.println(driver.getCurrentUrl());
        assertNotNull(connectPage1);
        assertEquals(CONNECT_FORM_TITLE,
                connectPage1.getTitle());

        // try to validate
        ConnectWizardPage connectPage2 = connectPage1.next(ConnectWizardPage.class);
        assertNotNull(connectPage2);
        assertEquals(
                "There were some errors in your form: You must define a login",
                connectPage2.getErrorMessage());

        // ok, let's try to skip the screen
        WizardPage connectSkip = connectPage1.navByLink(
                WizardPage.class, "Or skip and don't register", true);
        assertNotNull(connectSkip);
        assertEquals("You have not registered your instance on Nuxeo Connect.",
                connectSkip.getTitle2());

        // ok, let's register
        connectWizardPage = connectSkip.navById(WizardPage.class,
                "btnRetry", true);
        connectPage1 = connectWizardPage.getConnectPage(); // enter iframe again
        assertNotNull(connectPage1);

        // Register with a existing account
        ConnectWizardPage connectSignIn = connectPage1.getLink("click here");
        // Temporary workaround for spaces around "click here" link text
        if (connectSignIn == null) {
            System.out.println("Could not find link 'click here', trying with space");
            connectSignIn = connectPage1.getLink(" click here");
        }
        System.out.println(driver.getCurrentUrl());
        assertEquals("Pre-Register your new Nuxeo instance",
                connectSignIn.getTitle());
        // enter test login/password
        connectSignIn.fillInput("clogin", CONNECT_LOGIN);
        connectSignIn.fillInput("cpassword", getTestPassword());
        ConnectWizardPage connectProjectPage = connectSignIn.nav(
                ConnectWizardPage.class, "Continue");
        assertNotNull(connectProjectPage);

        // select the associated project
        connectProjectPage.selectOption("project", CONNECT_PROJECT_SELECTOR_UUID);
        //connectProjectPage.fillInput("project", CONNECT_PROJECT_SELECTOR);
        ConnectWizardPage connectFinish = connectProjectPage.nav(
                ConnectWizardPage.class, "Continue");
        assertNotNull(connectFinish);
        assertEquals("Your pre-registration has been done!",
                connectFinish.getTitle());

        // **********************
        // Exit Connect Form and Display Packages selection
        WizardPage packageSelectiondPage = connectFinish.nav(WizardPage.class,
                "Continue", true);
        assertNotNull(packageSelectiondPage);
        assertEquals("Select modules", packageSelectiondPage.getTitle());

        // use specific url
        String currentUrl = driver.getCurrentUrl();
        currentUrl = currentUrl + "?showPresets=true";
        packageSelectiondPage = get(currentUrl, WizardPage.class);

        WebElement presetBtn = findElementWithTimeout(By.id("preset_nuxeo-dm"));
        presetBtn.click();
        Thread.sleep(1000);

        // **************************
        // Package Download Screen

        WizardPage packageDownloadPage = packageSelectiondPage.next(true);
        assertNotNull(packageDownloadPage);
        assertEquals("Modules download", packageDownloadPage.getTitle());

        // **********************
        // Summary screen
        SummaryWizardPage summary = packageDownloadPage.next(SummaryWizardPage.class);
        assertNotNull(summary);
        assertEquals("Summary", summary.getTitle());
        assertNotNull(summary.getRegistration());

        // Restart
        LoginPage loginPage = summary.restart();

    }

    //@Test
    public void loopOnIframe() throws Exception {

        // **********************
        // welcome
        WizardPage welcomePage = get(NUXEO_URL, WizardPage.class);
        assertTrue(welcomePage.getTitle().contains("Welcome to "));

        // **********************
        // Settings
        WizardPage settingsPage = welcomePage.next();
        assertNotNull(settingsPage);

        assertEquals("General settings", settingsPage.getTitle());


        // **********************
        // proxy
        WizardPage proxyPage = settingsPage.next();
        assertNotNull(proxyPage);
        assertFalse(proxyPage.hasError());
        assertEquals("HTTP proxy settings", proxyPage.getTitle());

        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type", "none"));

        // **********************
        // Database settings
        WizardPage dbPage = proxyPage.next();
        assertNotNull(dbPage);
        assertFalse(dbPage.hasError());
        assertEquals("Database settings", dbPage.getTitle());

        // **********************
        // SMTP Settings
        WizardPage smtpPage = dbPage.next();
        assertNotNull(smtpPage);
        assertEquals("SMTP Settings", smtpPage.getTitle());
        assertTrue(smtpPage.selectOption("mail.smtp.auth", "false"));

        // **********************
        // Connect Form

        WizardPage connectWizardPage = smtpPage.next(WizardPage.class);

        for (int i = 1; i<20; i++) {

            assertNotNull(connectWizardPage);
            assertFalse(connectWizardPage.hasError());

            // enter embedded IFrame
            System.out.println(driver.getCurrentUrl());
            ConnectWizardPage connectPage1 = connectWizardPage.getConnectPage();
            System.out.println(driver.getCurrentUrl());
            assertNotNull(connectPage1);
            assertEquals(CONNECT_FORM_TITLE,
                    connectPage1.getTitle());

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
            assertEquals("You have not registered your instance on Nuxeo Connect.",
                    connectSkip.getTitle2());

            // ok, let's register
            connectWizardPage = connectSkip.navById(WizardPage.class,
                    "btnRetry");

        }

    }

    //@Test
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
        String smtpHost = systemPage.getConfig("mail.smtp.host");
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
        assertTrue(packageLink.getText().trim().toLowerCase().startsWith("restart"));

        updateCenterHome = packageListing.exit();
        assertNotNull(updateCenterHome);

        SystemHomePage systemHome = updateCenterHome.getSystemHomePage();
        assertNotNull(systemHome);

        LoginPage loginPage = systemHome.restart();

    }

    //@Test
    public void verifyPackageInstallation() throws Exception {

        DocumentBasePage home = login(NX_LOGIN, NX_PASSWORD);
        AdminCenterBasePage adminHome = home.getAdminCenter();
        assertNotNull(adminHome);

        SystemHomePage systemHomePage = adminHome.getSystemHomePage();
        AdminCenterBasePage distributions = systemHomePage.selectSubTab("Nuxeo distribution");

        WebElement bundle = findElementWithTimeout(By.xpath("//td[text()='org.nuxeo.ecm.platform.audit.web.access']"));
        assertNotNull(bundle);

    }

    //@Test
    public void studioPackageInstallAndUninstall() throws Exception {
        // XXX todo
    }


    @Test
    public void testAll() throws Exception {

        runWizardAndRestart();

        installPackageAndRestart();

        verifyPackageInstallation();
    }
}

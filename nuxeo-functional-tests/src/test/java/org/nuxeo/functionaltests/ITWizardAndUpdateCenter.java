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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;

public class ITWizardAndUpdateCenter extends AbstractTest {

    private static final String SMTP_SERVER_HOST = "someSMTPServer.com";

    private static final String SMTP_SERVER_PORT = "27";

    protected static final String NX_LOGIN = "Administrator";

    protected static final String NX_PASSWORD = "Administrator";

    protected static final String CONNECT_LOGIN = "junit4tester";

    protected static final String CONNECT_PROJECT_SELECTOR = "junit4tester";

    protected String getTestPassword() {
        return "XXX";
    }

    //@Test
    public void runWizardAndRestart() throws Exception {

        // **********************
        // welcome
        WizardPage welcomePage = get(NUXEO_URL, WizardPage.class);
        assertEquals("Welcome to Nuxeo DM", welcomePage.getTitle());

        // **********************
        // Settings
        WizardPage settingsPage = welcomePage.next();
        assertNotNull(settingsPage);

        assertEquals("General settings", settingsPage.getTitle());

        welcomePage = settingsPage.previous();
        assertNotNull(welcomePage);
        assertFalse(welcomePage.hasError());
        assertEquals("Welcome to Nuxeo DM", welcomePage.getTitle());

        // **********************
        // proxy
        WizardPage proxyPage = welcomePage.next().next();
        assertNotNull(proxyPage);
        assertFalse(proxyPage.hasError());
        assertEquals("HTTP proxy settings", proxyPage.getTitle());

        // check validation
        assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type", "anonymous"));
        proxyPage.clearInput("nuxeo.http.proxy.host");
        proxyPage.clearInput("nuxeo.http.proxy.port");
        proxyPage = proxyPage.next();
        assertTrue(proxyPage.hasError());

        proxyPage.fillInput("nuxeo.http.proxy.host", "myproxy");
        proxyPage.fillInput("nuxeo.http.proxy.port", "AAAA");
        proxyPage = proxyPage.next();
        assertTrue(proxyPage.hasError());

        proxyPage.fillInput("nuxeo.http.proxy.port", "8080");
        WizardPage somePage = proxyPage.next();
        assertFalse(somePage.hasError());
        proxyPage = somePage.previous();

        /*
         * assertTrue(proxyPage.selectOption("nuxeo.http.proxy.type",
         * "authenticated")); proxyPage.clearInput("nuxeo.http.proxy.login");
         * proxyPage.clearInput("nuxeo.http.proxy.password"); proxyPage =
         * proxyPage.next(); assertTrue(proxyPage.hasError());
         */
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
        // check port validation
        assertTrue(smtpPage.selectOption("mail.smtp.auth", "false"));
        smtpPage.fillInput("mail.smtp.host", SMTP_SERVER_HOST);
        smtpPage.fillInput("mail.smtp.port", "AAA");
        smtpPage = smtpPage.next(WizardPage.class);
        assertTrue(smtpPage.hasError());

        // check login/password validation
        smtpPage.fillInput("mail.smtp.port", SMTP_SERVER_PORT);
        assertTrue(smtpPage.selectOption("mail.smtp.auth", "true"));
        smtpPage.clearInput("mail.smtp.username");
        smtpPage.clearInput("mail.smtp.password");
        smtpPage = smtpPage.next(WizardPage.class);
        assertTrue(smtpPage.hasError());
        assertTrue(smtpPage.selectOption("mail.smtp.auth", "false"));

        // **********************
        // Connect Form
        WizardPage connectWizardPage = smtpPage.next(WizardPage.class);
        assertNotNull(connectWizardPage);
        assertFalse(connectWizardPage.hasError());

        // enter embedded IFrame
        System.out.println(driver.getCurrentUrl());
        ConnectWizardPage connectPage1 = connectWizardPage.getConnectPage();
        System.out.println(driver.getCurrentUrl());
        assertNotNull(connectPage1);
        assertEquals("Nuxeo Connect & Nuxeo Studio - Trial Offer",
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
        connectWizardPage = connectSkip.nav(WizardPage.class,
                "Okay, let's register");
        connectPage1 = connectWizardPage.getConnectPage(); // enter iframe again
        assertNotNull(connectPage1);

        // Register with a existing account
        System.out.println(driver.getCurrentUrl());
        ConnectWizardPage connectSignIn = connectPage1.getLink("I already have a connect account");
        System.out.println(driver.getCurrentUrl());
        assertEquals("Pre-Register your new Nuxeo instance",
                connectSignIn.getTitle());
        System.out.println(driver.getCurrentUrl());
        // enter test login/password
        connectSignIn.fillInput("clogin", CONNECT_LOGIN);
        connectSignIn.fillInput("cpassword", getTestPassword());
        ConnectWizardPage connectProjectPage = connectSignIn.nav(
                ConnectWizardPage.class, "Continue");
        assertNotNull(connectProjectPage);

        // select the associated project
        connectProjectPage.fillInput("project", CONNECT_PROJECT_SELECTOR);
        ConnectWizardPage connectFinish = connectProjectPage.nav(
                ConnectWizardPage.class, "Continue");
        assertNotNull(connectFinish);
        assertEquals("Your pre-registration has been done!",
                connectFinish.getTitle());

        // **********************
        // Summary screen
        SummaryWizardPage summary = connectFinish.nav(SummaryWizardPage.class,
                "Continue");
        assertNotNull(summary);
        assertEquals("Summary", summary.getTitle());

        // Restart
        LoginPage loginPage = summary.restart();

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

        // XXX Check setup parameters

        // Go to Update Center
        UpdateCenterPage updateCenterHome = connectHome.getUpdateCenterHomePage();
        updateCenterHome = updateCenterHome.nav(UpdateCenterPage.class,
                "Packages from Nuxeo Marketplace");

        // Get listing in IFrame
        PackageListingPage packageListing = updateCenterHome.getPackageListingPage();

        // Download Package
        WebElement link = packageListing.download("audit-web-access-1.0.0");
        assertNotNull(link);

        // Start installation
        PackageInstallationScreen installScreen = packageListing.getInstallationScreen("audit-web-access-1.0.0");
        assertNotNull(installScreen);

        packageListing = installScreen.start();
        assertNotNull(packageListing);

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

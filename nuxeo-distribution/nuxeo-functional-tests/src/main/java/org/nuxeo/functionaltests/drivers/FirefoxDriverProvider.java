/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Benoit Delbosc
 *     Antoine Taillefer
 *     Anahide Tchertchian
 *     Guillaume Renard
 *     Mathieu Guillaume
 *     Julien Carsique
 */
package org.nuxeo.functionaltests.drivers;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

/**
 * Driver provider for firefox.
 *
 * @since 8.2
 */
public class FirefoxDriverProvider implements DriverProvider {

    private static final Log log = LogFactory.getLog(FirefoxDriverProvider.class);

    protected FirefoxDriver driver;

    @Override
    public RemoteWebDriver init(DesiredCapabilities dc) throws Exception {
        FirefoxProfile profile = new FirefoxProfile();
        // Disable native events (makes things break on Windows)
        profile.setEnableNativeEvents(false);
        // Set English as default language
        profile.setPreference("general.useragent.locale", "en");
        profile.setPreference("intl.accept_languages", "en");
        // Set other confs to speed up FF

        // Speed up firefox by pipelining requests on a single connection
        profile.setPreference("network.http.keep-alive", true);
        profile.setPreference("network.http.pipelining", true);
        profile.setPreference("network.http.proxy.pipelining", true);
        profile.setPreference("network.http.pipelining.maxrequests", 8);

        // Try to use less memory
        profile.setPreference("browser.sessionhistory.max_entries", 10);
        profile.setPreference("browser.sessionhistory.max_total_viewers", 4);
        profile.setPreference("browser.sessionstore.max_tabs_undo", 4);
        profile.setPreference("browser.sessionstore.interval", 1800000);

        // disable unresponsive script alerts
        profile.setPreference("dom.max_script_run_time", 0);
        profile.setPreference("dom.max_chrome_script_run_time", 0);

        // don't skip proxy for localhost
        profile.setPreference("network.proxy.no_proxies_on", "");

        // prevent different kinds of popups/alerts
        profile.setPreference("browser.tabs.warnOnClose", false);
        profile.setPreference("browser.tabs.warnOnOpen", false);
        profile.setPreference("extensions.newAddons", false);
        profile.setPreference("extensions.update.notifyUser", false);

        // disable autoscrolling
        profile.setPreference("browser.urlbar.autocomplete.enabled", false);

        // downloads conf
        profile.setPreference("browser.download.useDownloadDir", false);

        // prevent FF from running in offline mode when there's no network
        // connection
        profile.setPreference("toolkit.networkmanager.disable", true);

        // prevent FF from giving health reports
        profile.setPreference("datareporting.policy.dataSubmissionEnabled", false);
        profile.setPreference("datareporting.healthreport.uploadEnabled", false);
        profile.setPreference("datareporting.healthreport.service.firstRun", false);
        profile.setPreference("datareporting.healthreport.service.enabled", false);
        profile.setPreference("datareporting.healthreport.logging.consoleEnabled", false);

        // start page conf to speed up FF
        profile.setPreference("browser.startup.homepage", "about:blank");
        profile.setPreference("pref.browser.homepage.disable_button.bookmark_page", false);
        profile.setPreference("pref.browser.homepage.disable_button.restore_default", false);

        // misc confs to avoid useless updates
        profile.setPreference("browser.search.update", false);
        profile.setPreference("browser.bookmarks.restore_default_bookmarks", false);

        // misc confs to speed up FF
        profile.setPreference("extensions.ui.dictionary.hidden", true);
        profile.setPreference("layout.spellcheckDefault", 0);
        // For FF > 40 ?
        profile.setPreference("startup.homepage_welcome_url.additional", "about:blank");

        // to ease up changing conf during tests
        profile.setPreference("general.warnOnAboutConfig", false);

        // webdriver logging
        if (Boolean.TRUE.equals(Boolean.valueOf(System.getenv("nuxeo.log.webriver")))) {
            String location = System.getProperty("basedir") + File.separator + "target";
            File outputFolder = new File(location);
            if (!outputFolder.exists() || !outputFolder.isDirectory()) {
                outputFolder = null;
            }
            File webdriverlogFile = File.createTempFile("webdriver", ".log", outputFolder);
            profile.setPreference("webdriver.log.file", webdriverlogFile.getAbsolutePath());
            log.warn("Webdriver logs saved in " + webdriverlogFile);
        }

        JavaScriptError.addExtension(profile);

        dc.setCapability(FirefoxDriver.PROFILE, profile);
        driver = new FirefoxDriver(dc);
        return driver;
    }

    @Override
    public RemoteWebDriver get() {
        return driver;
    }

    @Override
    public void quit() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

}

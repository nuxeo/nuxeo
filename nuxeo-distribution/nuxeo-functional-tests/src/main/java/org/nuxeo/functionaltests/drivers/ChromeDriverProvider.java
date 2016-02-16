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
import java.util.Arrays;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Driver provider for chrome.
 *
 * @since 8.2
 */
public class ChromeDriverProvider implements DriverProvider {

    private static final Log log = LogFactory.getLog(DriverProvider.class);

    public static final String CHROME_DRIVER_DEFAULT_PATH_LINUX = "/usr/bin/chromedriver";

    /**
     * "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" doesn't work
     */
    public static final String CHROME_DRIVER_DEFAULT_PATH_MAC = "/Applications/chromedriver";

    public static final String CHROME_DRIVER_DEFAULT_PATH_WINVISTA = SystemUtils.getUserHome().getPath()
            + "\\AppData\\Local\\Google\\Chrome\\Application\\chromedriver.exe";

    public static final String CHROME_DRIVER_DEFAULT_PATH_WINXP = SystemUtils.getUserHome().getPath()
            + "\\Local Settings\\Application Data\\Google\\Chrome\\Application\\chromedriver.exe";

    public static final String CHROME_DRIVER_DEFAULT_EXECUTABLE_NAME = "chromedriver";

    public static final String CHROME_DRIVER_WINDOWS_EXECUTABLE_NAME = "chromedriver.exe";

    public static final String SYSPROP_CHROME_DRIVER_PATH = "webdriver.chrome.driver";

    protected ChromeDriver driver;

    @Override
    public RemoteWebDriver init(DesiredCapabilities dc) throws Exception {
        if (System.getProperty(SYSPROP_CHROME_DRIVER_PATH) == null) {
            String chromeDriverDefaultPath = null;
            String chromeDriverExecutableName = CHROME_DRIVER_DEFAULT_EXECUTABLE_NAME;
            if (SystemUtils.IS_OS_LINUX) {
                chromeDriverDefaultPath = CHROME_DRIVER_DEFAULT_PATH_LINUX;
            } else if (SystemUtils.IS_OS_MAC) {
                chromeDriverDefaultPath = CHROME_DRIVER_DEFAULT_PATH_MAC;
            } else if (SystemUtils.IS_OS_WINDOWS_XP) {
                chromeDriverDefaultPath = CHROME_DRIVER_DEFAULT_PATH_WINXP;
                chromeDriverExecutableName = CHROME_DRIVER_WINDOWS_EXECUTABLE_NAME;
            } else if (SystemUtils.IS_OS_WINDOWS_VISTA) {
                chromeDriverDefaultPath = CHROME_DRIVER_DEFAULT_PATH_WINVISTA;
                chromeDriverExecutableName = CHROME_DRIVER_WINDOWS_EXECUTABLE_NAME;
            } else if (SystemUtils.IS_OS_WINDOWS) {
                // Unknown default path on other Windows OS. To be completed.
                chromeDriverExecutableName = CHROME_DRIVER_WINDOWS_EXECUTABLE_NAME;
            }

            if (chromeDriverDefaultPath != null && new File(chromeDriverDefaultPath).exists()) {
                log.warn(String.format("Missing property %s but found %s. Using it...", SYSPROP_CHROME_DRIVER_PATH,
                        chromeDriverDefaultPath));
                System.setProperty(SYSPROP_CHROME_DRIVER_PATH, chromeDriverDefaultPath);
            } else {
                // Can't find chromedriver in default location, check system
                // path
                File chromeDriverExecutable = findExecutableOnPath(chromeDriverExecutableName);
                if ((chromeDriverExecutable != null) && (chromeDriverExecutable.exists())) {
                    log.warn(String.format("Missing property %s but found %s. Using it...", SYSPROP_CHROME_DRIVER_PATH,
                            chromeDriverExecutable.getCanonicalPath()));
                    System.setProperty(SYSPROP_CHROME_DRIVER_PATH, chromeDriverExecutable.getCanonicalPath());
                } else {
                    log.error(String.format(
                            "Could not find the Chrome driver looking at %s or system path."
                                    + " Download it from %s and set its path with " + "the System property %s.",
                            chromeDriverDefaultPath, "http://code.google.com/p/chromedriver/downloads/list",
                            SYSPROP_CHROME_DRIVER_PATH));
                }
            }
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments(Arrays.asList("--ignore-certificate-errors"));
        dc.setCapability(ChromeOptions.CAPABILITY, options);
        driver = new ChromeDriver(dc);
        return driver;
    }

    /**
     * @since 5.7
     */
    protected static File findExecutableOnPath(String executableName) {
        String systemPath = System.getenv("PATH");
        String[] pathDirs = systemPath.split(File.pathSeparator);
        File fullyQualifiedExecutable = null;
        for (String pathDir : pathDirs) {
            File file = new File(pathDir, executableName);
            if (file.isFile()) {
                fullyQualifiedExecutable = file;
                break;
            }
        }
        return fullyQualifiedExecutable;
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

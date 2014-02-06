/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

/**
 * Helper class to take screenshots.
 * <p>
 * Allows taking a png screenshot and the HTML source of the page.
 * <p>
 * Files are saved in a file in the maven base dir/target, and fallback on the
 * the system temp folder if can't find it.
 * <p>
 * This temp file won't be deleted on exist.
 *
 * @since 5.9.2
 */
public class ScreenshotTaker {

    private static final Log log = LogFactory.getLog(ScreenshotTaker.class);

    protected final String targetDirName;

    public ScreenshotTaker() {
        this(null);
    }

    public ScreenshotTaker(String targetDirName) {
        super();
        this.targetDirName = targetDirName;
    }

    public File takeScreenshot(WebDriver driver, String filename) {
        if (driver == null) {
            return null;
        }
        if (TakesScreenshot.class.isInstance(driver)) {
            try {
                Thread.sleep(250);
                String name = filename;
                return TakesScreenshot.class.cast(driver).getScreenshotAs(
                        new ScreenShotFileOutput(targetDirName, name));
            } catch (InterruptedException e) {
                log.error(e, e);
            }
        }
        return null;
    }

    public File dumpPageSource(WebDriver driver, String filename) {
        if (driver == null) {
            return null;
        }
        FileWriter writer = null;
        try {
            String location = System.getProperty("basedir") + File.separator
                    + "target";
            File outputFolder = new File(location);
            if (!outputFolder.exists() || !outputFolder.isDirectory()) {
                outputFolder = null;
            }
            if (outputFolder != null && !StringUtils.isBlank(targetDirName)) {
                outputFolder = new File(outputFolder, targetDirName);
                outputFolder.mkdir();
            }
            File tmpFile = File.createTempFile(filename, ".html", outputFolder);
            log.trace(String.format("Created page source file named '%s'",
                    tmpFile.getPath()));
            writer = new FileWriter(tmpFile);
            writer.write(driver.getPageSource());
            return tmpFile;
        } catch (IOException e) {
            throw new WebDriverException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

}

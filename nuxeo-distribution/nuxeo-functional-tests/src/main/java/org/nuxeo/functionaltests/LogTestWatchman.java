/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */

package org.nuxeo.functionaltests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.nuxeo.common.utils.URIUtils;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Watchman to log info about the test and create snapshot on failure.
 *
 * @since 5.8
 */
public class LogTestWatchman extends TestWatchman {

    protected static final Log log = LogFactory.getLog(AbstractTest.class);

    protected String lastScreenshot;

    protected String lastPageSource;

    protected String filePrefix;

    protected RemoteWebDriver driver;

    protected String serverURL;

    public LogTestWatchman(final RemoteWebDriver driver, final String serverURL) {
        this.driver = driver;
        this.serverURL = serverURL;
    }

    public LogTestWatchman() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Statement apply(final Statement base, final FrameworkMethod method,
            Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                starting(method);
                try {
                    if (base instanceof RunAfters) {
                        // Hack JUnit: in order to take screenshot at the right
                        // time we add through reflection an after
                        // function that will be executed before all other
                        // ones. See NXP-12742
                        Field fAtersField = RunAfters.class.getDeclaredField("fAfters");
                        fAtersField.setAccessible(true);

                        List<FrameworkMethod> afters = (List<FrameworkMethod>) fAtersField.get(base);
                        if (afters != null && !afters.isEmpty()) {
                            try {
                                // Improve this and instead of finding a
                                // special function, we could register functions
                                // specially annotated.
                                FrameworkMethod first = afters.get(0);
                                Method m = AbstractTest.class.getMethod(
                                        "runBeforeAfters", (Class<?>[]) null);
                                FrameworkMethod f = new FrameworkMethod(m);
                                if (first != null && !first.equals(f)) {
                                    afters.add(0, f);
                                }
                            } catch (NoSuchMethodException e) {
                                // Do nothing
                            }
                        }
                    }
                    base.evaluate();
                    succeeded(method);
                } catch (Throwable t) {
                    failed(t, method);
                    throw t;
                } finally {
                    finished(method);
                }
            }
        };
    }

    public File dumpPageSource(String filename) {
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

    @Override
    public void failed(Throwable e, FrameworkMethod method) {
        String className = getTestClassName(method);
        String methodName = method.getName();
        log.error(String.format("Test '%s#%s' failed", className, methodName),
                e);

        if (lastScreenshot == null || lastPageSource == null) {
            if (lastScreenshot == null) {
                File temp = takeScreenshot(filePrefix);
                lastScreenshot = temp != null ? temp.getAbsolutePath() : null;
            }

            if (lastPageSource == null) {
                File temp = dumpPageSource(filePrefix);
                lastPageSource = temp != null ? temp.getAbsolutePath() : null;
            }

        }
        log.info(String.format("Created screenshot file named '%s'",
                lastScreenshot));
        log.info(String.format("Created page source file named '%s'",
                lastPageSource));
        super.failed(e, method);
    }

    @Override
    public void finished(FrameworkMethod method) {
        log.info(String.format("Finished test '%s#%s'",
                getTestClassName(method), method.getName()));
        lastScreenshot = null;
        lastPageSource = null;
        super.finished(method);
    }

    public RemoteWebDriver getDriver() {
        return driver;
    }

    public String getServerURL() {
        return serverURL;
    }

    protected String getTestClassName(FrameworkMethod method) {
        return method.getMethod().getDeclaringClass().getName();
    }

    protected void logOnServer(String message) {
        if (driver != null) {
            driver.get(String.format(
                    "%s/restAPI/systemLog?token=dolog&level=WARN&message=----- WebDriver: %s",
                    serverURL, URIUtils.quoteURIPathComponent(message, true)));
        } else {
            log.warn(String.format("Cannot log on server message: %s", message));
        }
    }

    public void runBeforeAfters() {
        lastScreenshot = takeScreenshot(filePrefix).getAbsolutePath();
        lastPageSource = dumpPageSource(filePrefix).getAbsolutePath();
    }

    public void setDriver(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    @Override
    public void starting(FrameworkMethod method) {
        String message = String.format("Starting test '%s#%s'",
                getTestClassName(method), method.getName());
        log.info(message);
        String className = getTestClassName(method);
        String methodName = method.getName();
        filePrefix = String.format("screenshot-lastpage-%s-%s", className,
                methodName);
        logOnServer(message);
    }

    @Override
    public void succeeded(FrameworkMethod method) {
        if (lastPageSource != null) {
            new File(lastPageSource).delete();
        }
        if (lastScreenshot != null) {
            new File(lastScreenshot).delete();
        }
    }

    public File takeScreenshot(String filename) {
        if (TakesScreenshot.class.isInstance(driver)) {
            try {
                Thread.sleep(250);
                return TakesScreenshot.class.cast(driver).getScreenshotAs(
                        new ScreenShotFileOutput(filename));
            } catch (InterruptedException e) {
                log.error(e, e);
            }
        }
        return null;
    }

}

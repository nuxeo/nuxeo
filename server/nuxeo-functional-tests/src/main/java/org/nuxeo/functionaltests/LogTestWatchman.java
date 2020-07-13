/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.nuxeo.client.NuxeoClient;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Watchman to log info about the test and create snapshot on failure.
 *
 * @since 5.8
 */
public class LogTestWatchman extends TestWatchman {

    private static final Log log = LogFactory.getLog(LogTestWatchman.class);

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
    public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
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
                        Field aftersField = RunAfters.class.getDeclaredField("afters");
                        aftersField.setAccessible(true);

                        List<FrameworkMethod> afters = (List<FrameworkMethod>) aftersField.get(base);
                        if (afters != null && !afters.isEmpty()) {
                            try {
                                // Improve this and instead of finding a
                                // special function, we could register
                                // functions specially annotated.
                                FrameworkMethod first = afters.get(0);
                                Method m = AbstractTest.class.getMethod("runBeforeAfters", (Class<?>[]) null);
                                FrameworkMethod f = new FrameworkMethod(m);
                                if (first != null && !first.equals(f)) {
                                    for (;;) {
                                        String aftersClassName = afters.getClass().getName();
                                        if (aftersClassName.endsWith("$UnmodifiableList")
                                                || aftersClassName.endsWith("$UnmodifiableRandomAccessList")) {
                                            Class<?> unmodifiableListClass = Collections.unmodifiableList(
                                                    new LinkedList<>()).getClass();
                                            Field listField = unmodifiableListClass.getDeclaredField("list");
                                            listField.setAccessible(true);
                                            afters = (List<FrameworkMethod>) listField.get(afters);
                                        } else {
                                            break;
                                        }
                                    }
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

    @Override
    public void failed(Throwable e, FrameworkMethod method) {
        String className = getTestClassName(method);
        String methodName = method.getName();
        log.error(String.format("Test '%s#%s' failed", className, methodName), e);

        if (lastScreenshot == null || lastPageSource == null) {
            ScreenshotTaker taker = new ScreenshotTaker();

            if (lastScreenshot == null) {
                File temp = taker.takeScreenshot(driver, filePrefix);
                lastScreenshot = temp != null ? temp.getAbsolutePath() : null;
            }

            if (lastPageSource == null) {
                File temp = taker.dumpPageSource(driver, filePrefix);
                lastPageSource = temp != null ? temp.getAbsolutePath() : null;
            }

        }
        log.info(String.format("Created screenshot file named '%s'", lastScreenshot));
        log.info(String.format("Created page source file named '%s'", lastPageSource));
        super.failed(e, method);
    }

    @Override
    public void finished(FrameworkMethod method) {
        log.info(String.format("Finished test '%s#%s'", getTestClassName(method), method.getName()));
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
        RestHelper.logOnServer(message);
    }

    public void runBeforeAfters() {
        if (driver != null) {
            ScreenshotTaker taker = new ScreenshotTaker();
            lastScreenshot = taker.takeScreenshot(driver, filePrefix).getAbsolutePath();
            lastPageSource = taker.dumpPageSource(driver, filePrefix).getAbsolutePath();
        }
    }

    public void setDriver(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    @Override
    public void starting(FrameworkMethod method) {
        String message = String.format("Starting test '%s#%s'", getTestClassName(method), method.getName());
        log.info(message);
        String className = getTestClassName(method);
        String methodName = method.getName();
        filePrefix = String.format("screenshot-lastpage-%s-%s", className, methodName);
        logOnServer(message);
    }

    @Override
    public void succeeded(FrameworkMethod method) {
        if (lastPageSource != null && !new File(lastPageSource).delete()) {
            log.warn("file deletion failed for: " + lastPageSource);
        }
        if (lastScreenshot != null && !new File(lastScreenshot).delete()) {
            log.warn("file deletion failed for: " + lastScreenshot);
        }
    }

    /**
     * We need this class in order to make simple HTTP GET calls to know if we can log or not.
     * <p />
     * For some tests, REST API is not available, so basic client didn't succeed to connect.
     *
     * @since 9.3
     */
    public static class NuxeoClientForWebDriver extends NuxeoClient {

        protected NuxeoClientForWebDriver(Builder builder) {
            super(builder);
        }
    }

}

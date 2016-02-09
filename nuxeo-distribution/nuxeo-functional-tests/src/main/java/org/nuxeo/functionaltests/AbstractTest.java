/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Benoit Delbosc
 *     Antoine Taillefer
 *     Anahide Tchertchian
 *     Guillaume Renard
 *     Mathieu Guillaume
 *     Julien Carsique
 */
package org.nuxeo.functionaltests;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.browsermob.proxy.ProxyServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.MethodRule;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.functionaltests.fragment.WebFragment;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.nuxeo.functionaltests.pages.NoteDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.CollectionCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.DublinCoreCreationDocumentFormPage;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.NoteCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceFormPage;
import org.nuxeo.functionaltests.pages.tabs.CollectionContentTabSubPage;
import org.nuxeo.runtime.api.Framework;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

/**
 * Base functions for all pages.
 */
public abstract class AbstractTest {

    /**
     * @since 5.9.2
     */
    public final static String TEST_USERNAME = "jdoe";

    /**
     * @since 5.9.2
     */
    public final static String TEST_PASSWORD = "test";

    /**
     * Polling frequency in milliseconds.
     *
     * @since 5.9.2
     */
    public static final int POLLING_FREQUENCY_MILLISECONDS = 100;

    /**
     * Page Load timeout in seconds.
     *
     * @since 5.9.2
     */
    public static final int PAGE_LOAD_TIME_OUT_SECONDS = 60;

    /**
     * @since 5.7
     */
    public static final String CHROME_DRIVER_DEFAULT_PATH_LINUX = "/usr/bin/chromedriver";

    /**
     * @since 5.7 "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" doesn't work
     */
    public static final String CHROME_DRIVER_DEFAULT_PATH_MAC = "/Applications/chromedriver";

    /**
     * @since 5.7
     */
    public static final String CHROME_DRIVER_DEFAULT_PATH_WINVISTA = SystemUtils.getUserHome().getPath()
            + "\\AppData\\Local\\Google\\Chrome\\Application\\chromedriver.exe";

    /**
     * @since 5.7
     */
    public static final String CHROME_DRIVER_DEFAULT_PATH_WINXP = SystemUtils.getUserHome().getPath()
            + "\\Local Settings\\Application Data\\Google\\Chrome\\Application\\chromedriver.exe";

    /**
     * @since 5.7
     */
    public static final String CHROME_DRIVER_DEFAULT_EXECUTABLE_NAME = "chromedriver";

    /**
     * @since 5.7
     */
    public static final String CHROME_DRIVER_WINDOWS_EXECUTABLE_NAME = "chromedriver.exe";

    static final Log log = LogFactory.getLog(AbstractTest.class);

    public static final String NUXEO_URL = System.getProperty("nuxeoURL", "http://localhost:8080/nuxeo").replaceAll(
            "/$", "");

    public static final int LOAD_TIMEOUT_SECONDS = 30;

    public static final int LOAD_SHORT_TIMEOUT_SECONDS = 2;

    public static final int AJAX_TIMEOUT_SECONDS = 10;

    public static final int AJAX_SHORT_TIMEOUT_SECONDS = 2;

    public static final int POLLING_FREQUENCY_SECONDS = 1;

    private static final String FIREBUG_XPI = "firebug-1.6.2-fx.xpi";

    private static final String FIREBUG_VERSION = "1.6.2";

    private static final String FIREBUG_M2 = "firebug/firebug/1.6.2-fx";

    private static final int PROXY_PORT = 4444;

    private static final String HAR_NAME = "http-headers.json";

    public static final String SYSPROP_CHROME_DRIVER_PATH = "webdriver.chrome.driver";

    public static RemoteWebDriver driver;

    protected static File tmp_firebug_xpi;

    protected static ProxyServer proxyServer = null;

    /**
     * Logger method to follow what's being run on server logs and take a screenshot of the last page in case of failure
     */
    @Rule
    public MethodRule watchman = new LogTestWatchman(driver, NUXEO_URL);

    /**
     * This method will be executed before any method registered with JUnit After annotation.
     *
     * @since 5.8
     */
    public void runBeforeAfters() {
        ((LogTestWatchman) watchman).runBeforeAfters();
    }

    @BeforeClass
    public static void initDriver() throws Exception {
        String browser = System.getProperty("browser", "firefox");
        // Use the same strings as command-line Selenium
        if (browser.equals("chrome") || browser.equals("firefox")) {
            initFirefoxDriver();
        } else if (browser.equals("googlechrome")) {
            initChromeDriver();
        } else {
            throw new RuntimeException("Browser not supported: " + browser);
        }
        driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIME_OUT_SECONDS, TimeUnit.SECONDS);
    }

    protected static void initFirefoxDriver() throws Exception {
        DesiredCapabilities dc = DesiredCapabilities.firefox();
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

        addFireBug(profile);
        JavaScriptError.addExtension(profile);
        Proxy proxy = startProxy();
        if (proxy != null) {
            // Does not work, but leave code for when it does
            // Workaround: use 127.0.0.2
            proxy.setNoProxy("");
            // setProxyPreferences method does not exist with selenium version 2.43.0
            // profile.setProxyPreferences(proxy);
            dc.setCapability(CapabilityType.PROXY, proxy);
        }
        dc.setCapability(FirefoxDriver.PROFILE, profile);
        driver = new FirefoxDriver(dc);
    }

    protected static void initChromeDriver() throws Exception {
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
                    log.error(String.format("Could not find the Chrome driver looking at %s or system path."
                            + " Download it from %s and set its path with " + "the System property %s.",
                            chromeDriverDefaultPath, "http://code.google.com/p/chromedriver/downloads/list",
                            SYSPROP_CHROME_DRIVER_PATH));
                }
            }
        }
        DesiredCapabilities dc = DesiredCapabilities.chrome();
        ChromeOptions options = new ChromeOptions();
        options.addArguments(Arrays.asList("--ignore-certificate-errors"));
        Proxy proxy = startProxy();
        if (proxy != null) {
            proxy.setNoProxy("");
            dc.setCapability(CapabilityType.PROXY, proxy);
        }
        dc.setCapability(ChromeOptions.CAPABILITY, options);
        driver = new ChromeDriver(dc);
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

    /**
     * @since 7.1
     */
    @After
    public void checkJavascriptError() {
        if (driver != null) {
            new JavaScriptErrorCollector(driver).checkForErrors();
        }
    }

    @AfterClass
    public static void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
        removeFireBug();

        try {
            stopProxy();
        } catch (Exception e) {
            log.error("Could not stop proxy: " + e.getMessage());
        }
    }

    /**
     * Introspects the classpath and returns the list of files in it. FIXME: should use
     * HarnessRuntime#getClassLoaderFiles that returns the same thing
     *
     * @throws Exception
     */
    protected static List<String> getClassLoaderFiles() throws Exception {
        ClassLoader cl = AbstractTest.class.getClassLoader();
        URL[] urls = null;
        if (cl instanceof URLClassLoader) {
            urls = ((URLClassLoader) cl).getURLs();
        } else if (cl.getClass().getName().equals("org.apache.tools.ant.AntClassLoader")) {
            Method method = cl.getClass().getMethod("getClasspath");
            String cp = (String) method.invoke(cl);
            String[] paths = cp.split(File.pathSeparator);
            urls = new URL[paths.length];
            for (int i = 0; i < paths.length; i++) {
                urls[i] = new URL("file:" + paths[i]);
            }
        } else {
            System.err.println("Unknown classloader type: " + cl.getClass().getName());
            return null;
        }

        JarFile surefirebooterJar = null;
        for (URL url : urls) {
            URI uri = url.toURI();
            if (uri.getPath().matches(".*/nuxeo-runtime-[^/]*\\.jar")) {
                break;
            } else if (uri.getScheme().equals("file") && uri.getPath().contains("surefirebooter")) {
                surefirebooterJar = new JarFile(new File(uri));
            }
        }

        // special case for maven surefire with useManifestOnlyJar
        if (surefirebooterJar != null) {
            try {
                try {
                    String cp = surefirebooterJar.getManifest()
                                                 .getMainAttributes()
                                                 .getValue(Attributes.Name.CLASS_PATH);
                    if (cp != null) {
                        String[] cpe = cp.split(" ");
                        URL[] newUrls = new URL[cpe.length];
                        for (int i = 0; i < cpe.length; i++) {
                            // Don't need to add 'file:' with maven
                            // surefire >= 2.4.2
                            String newUrl = cpe[i].startsWith("file:") ? cpe[i] : "file:" + cpe[i];
                            newUrls[i] = new URL(newUrl);
                        }
                        urls = newUrls;
                    }
                } finally {
                    surefirebooterJar.close();
                }
            } catch (Exception e) {
                // skip
            }
        }
        // turn into files
        List<String> files = new ArrayList<>(urls.length);
        for (URL url : urls) {
            files.add(url.toURI().getPath());
        }
        return files;
    }

    private static final String M2_REPO = "repository/";

    protected static void addFireBug(FirefoxProfile profile) throws Exception {
        // this is preventing from running tests in eclipse
        // profile.addExtension(AbstractTest.class, "/firebug.xpi");

        File xpi = null;
        List<String> clf = getClassLoaderFiles();
        for (String f : clf) {
            if (f.endsWith("/" + FIREBUG_XPI)) {
                xpi = new File(f);
            }
        }
        if (xpi == null) {
            String customM2Repo = System.getProperty("M2_REPO", M2_REPO).replaceAll("/$", "");
            // try to guess the location in the M2 repo
            for (String f : clf) {
                if (f.contains(customM2Repo)) {
                    String m2 = f.substring(0, f.indexOf(customM2Repo) + customM2Repo.length());
                    xpi = new File(m2 + "/" + FIREBUG_M2 + "/" + FIREBUG_XPI);
                    break;
                }
            }
        }
        if (xpi == null || !xpi.exists()) {
            log.warn(FIREBUG_XPI + " not found in classloader or local M2 repository");
            return;
        }
        profile.addExtension(xpi);

        // avoid "first run" page
        profile.setPreference("extensions.firebug.currentVersion", FIREBUG_VERSION);
    }

    protected static void removeFireBug() {
        if (tmp_firebug_xpi != null) {
            tmp_firebug_xpi.delete();
            tmp_firebug_xpi.getParentFile().delete();
        }
    }

    protected static Proxy startProxy() throws Exception {
        if (Boolean.TRUE.equals(Boolean.valueOf(System.getProperty("useProxy", "false")))) {
            proxyServer = new ProxyServer(PROXY_PORT);
            proxyServer.start();
            proxyServer.setCaptureHeaders(true);
            // Block access to tracking sites
            proxyServer.blacklistRequests("https?://www\\.nuxeo\\.com/embedded/wizard.*", 410);
            proxyServer.blacklistRequests("https?://.*\\.mktoresp\\.com/.*", 410);
            proxyServer.blacklistRequests(".*_mchId.*", 410);
            proxyServer.blacklistRequests("https?://.*\\.google-analytics\\.com/.*", 410);
            proxyServer.newHar("webdriver-test");
            Proxy proxy = proxyServer.seleniumProxy();
            return proxy;
        } else {
            return null;
        }
    }

    protected static void stopProxy() throws Exception {
        if (proxyServer != null) {
            String target = System.getProperty(Environment.NUXEO_LOG_DIR);
            File harFile;
            if (target == null) {
                harFile = new File(HAR_NAME);
            } else {
                harFile = new File(target, HAR_NAME);
            }
            proxyServer.getHar().writeTo(harFile);
            proxyServer.stop();
        }
    }

    public static <T> T get(String url, Class<T> pageClassToProxy) {
        if (driver != null) {
            new JavaScriptErrorCollector(driver).checkForErrors();
        }
        driver.get(url);
        return asPage(pageClassToProxy);
    }


    public static void open(String url){
        if (driver != null) {
            new JavaScriptErrorCollector(driver).checkForErrors();
        }
        driver.get(NUXEO_URL + url + "?conversationId=0NXMAIN");
    }

    /**
     * Do not wait for page load. Do not handle error. Do not give explicit error in case of failure. This is a very raw
     * get.
     *
     * @since 6.0
     */
    public static <T> T getWithoutErrorHandler(String url, Class<T> pageClassToProxy) throws IOException {
        Command command = new Command(AbstractTest.driver.getSessionId(), DriverCommand.GET,
                ImmutableMap.of("url", url));
        AbstractTest.driver.getCommandExecutor().execute(command);
        return asPage(pageClassToProxy);
    }

    public static WebDriver getPopup() {
        String currentWindow = driver.getWindowHandle();
        for (String popup : driver.getWindowHandles()) {
            if (popup.equals(currentWindow)) {
                continue;
            }
            return driver.switchTo().window(popup);
        }
        return null;
    }

    public static <T> T asPage(Class<T> pageClassToProxy) {
        T page = instantiatePage(pageClassToProxy);
        return fillElement(pageClassToProxy, page);
    }

    public static <T extends WebFragment> T getWebFragment(By by, Class<T> webFragmentClass) {
        WebElement element = Locator.findElementWithTimeout(by);
        return getWebFragment(element, webFragmentClass);
    }

    public static <T extends WebFragment> T getWebFragment(WebElement element, Class<T> webFragmentClass) {
        T webFragment = instantiateWebFragment(element, webFragmentClass);
        webFragment = fillElement(webFragmentClass, webFragment);
        // fillElement somehow overwrite the 'element' field, reset it.
        webFragment.setElement(element);
        return webFragment;
    }

    /**
     * Fills an instantiated page/form/widget attributes
     *
     * @since 5.7
     */
    public static <T> T fillElement(Class<T> pageClassToProxy, T page) {
        PageFactory.initElements(new VariableElementLocatorFactory(driver, AJAX_TIMEOUT_SECONDS), page);
        // check all required WebElements on the page and wait for their
        // loading
        final List<String> fieldNames = new ArrayList<>();
        final List<WrapsElement> elements = new ArrayList<>();
        for (Field field : pageClassToProxy.getDeclaredFields()) {
            if (field.getAnnotation(Required.class) != null) {
                try {
                    field.setAccessible(true);
                    fieldNames.add(field.getName());
                    elements.add((WrapsElement) field.get(page));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Wait<T> wait = new FluentWait<>(page).withTimeout(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS);
        try {
            return wait.until(new Function<T, T>() {
                @Override
                public T apply(T aPage) {
                    String notLoaded = anyElementNotLoaded(elements, fieldNames);
                    if (notLoaded == null) {
                        return aPage;
                    } else {
                        return null;
                    }
                }
            });
        } catch (TimeoutException e) {
            throw new TimeoutException("not loaded: " + anyElementNotLoaded(elements, fieldNames), e);
        }
    }

    protected static String anyElementNotLoaded(List<WrapsElement> proxies, List<String> fieldNames) {
        for (int i = 0; i < proxies.size(); i++) {
            WrapsElement proxy = proxies.get(i);
            try {
                // method implemented in LocatingElementHandler
                proxy.getWrappedElement();
            } catch (NoSuchElementException e) {
                return fieldNames.get(i);
            }
        }
        return null;
    }

    // private in PageFactory...
    protected static <T> T instantiatePage(Class<T> pageClassToProxy) {
        try {
            try {
                Constructor<T> constructor = pageClassToProxy.getConstructor(WebDriver.class);
                return constructor.newInstance(driver);
            } catch (NoSuchMethodException e) {
                return pageClassToProxy.newInstance();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static <T extends WebFragment> T instantiateWebFragment(WebElement element, Class<T> webFragmentClass) {
        try {
            try {
                Constructor<T> constructor = webFragmentClass.getConstructor(WebDriver.class, WebElement.class);
                return constructor.newInstance(driver, element);
            } catch (NoSuchMethodException e) {
                return webFragmentClass.newInstance();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public LoginPage getLoginPage() {
        return get(NUXEO_URL + "/logout", LoginPage.class);
    }

    public LoginPage logout() {
        return getLoginPage();
    }

    /**
     * navigate to a link text. wait until the link is available and click on it.
     */
    public <T extends AbstractPage> T nav(Class<T> pageClass, String linkText) {
        WebElement link = Locator.findElementWithTimeout(By.linkText(linkText));
        if (link == null) {
            return null;
        }
        link.click();
        return asPage(pageClass);
    }

    /**
     * Navigate to a specified url
     *
     * @param urlString url
     * @throws MalformedURLException
     */
    public void navToUrl(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        driver.navigate().to(url);
    }

    /**
     * Login as Administrator
     *
     * @return the Document base page (by default returned by CAP)
     * @throws UserNotConnectedException
     */
    public DocumentBasePage login() throws UserNotConnectedException {
        return login("Administrator", "Administrator");
    }

    public DocumentBasePage login(String username, String password) throws UserNotConnectedException {
        DocumentBasePage documentBasePage = getLoginPage().login(username, password, DocumentBasePage.class);
        documentBasePage.checkUserConnected(username);
        return documentBasePage;
    }

    /**
     * Login as default test user.
     *
     * @since 5.9.2
     */
    public DocumentBasePage loginAsTestUser() throws UserNotConnectedException {
        return login(TEST_USERNAME, TEST_PASSWORD);
    }

    /**
     * Login using an invalid credential.
     *
     * @param username
     * @param password
     */
    public LoginPage loginInvalid(String username, String password) {
        LoginPage loginPage = getLoginPage().login(username, password, LoginPage.class);
        return loginPage;
    }

    /**
     * Init the repository with a test Workspace form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @return the created Workspace page
     * @throws Exception if initializing repository fails
     */
    protected DocumentBasePage initRepository(DocumentBasePage currentPage) throws Exception {
        return createWorkspace(currentPage, "Test Workspace", "Test Workspace for my dear WebDriver.");
    }

    /**
     * Cleans the repository (delete the test Workspace) from the {@code currentPage}.
     *
     * @param currentPage the current page
     * @throws Exception if cleaning repository fails
     */
    protected void cleanRepository(DocumentBasePage currentPage) throws Exception {
        deleteWorkspace(currentPage, "Test Workspace");
    }

    /**
     * Creates a Workspace form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param workspaceTitle the workspace title
     * @param workspaceDescription the workspace description
     * @return the created Workspace page
     */
    protected DocumentBasePage createWorkspace(DocumentBasePage currentPage, String workspaceTitle,
            String workspaceDescription) {
        // Go to Workspaces
        DocumentBasePage workspacesPage = currentPage.getNavigationSubPage().goToDocument("Workspaces");
        // Get Workspace creation form page
        WorkspaceFormPage workspaceCreationFormPage = workspacesPage.getWorkspacesContentTab().getWorkspaceCreatePage();
        // Create Workspace
        DocumentBasePage workspacePage = workspaceCreationFormPage.createNewWorkspace(workspaceTitle,
                workspaceDescription);
        return workspacePage;
    }

    /**
     * Deletes the Workspace with title {@code workspaceTitle} from the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param workspaceTitle the workspace title
     */
    protected void deleteWorkspace(DocumentBasePage currentPage, String workspaceTitle) {
        // Go to Workspaces
        DocumentBasePage workspacesPage = currentPage.getNavigationSubPage().goToDocument("Workspaces");
        // Delete the Workspace
        workspacesPage.getContentTab().removeDocument(workspaceTitle);
    }

    /**
     * Creates a File form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param fileTitle the file title
     * @param fileDescription the file description
     * @param uploadBlob true if a blob needs to be uploaded (temporary file created for this purpose)
     * @param filePrefix the file prefix
     * @param fileSuffix the file suffix
     * @param fileContent the file content
     * @return the created File page
     * @throws IOException if temporary file creation fails
     */
    protected FileDocumentBasePage createFile(DocumentBasePage currentPage, String fileTitle, String fileDescription,
            boolean uploadBlob, String filePrefix, String fileSuffix, String fileContent) throws IOException {
        // Get File creation form page
        FileCreationFormPage fileCreationFormPage = currentPage.getContentTab().getDocumentCreatePage("File",
                FileCreationFormPage.class);
        // Create File
        FileDocumentBasePage filePage = fileCreationFormPage.createFileDocument(fileTitle, fileDescription, uploadBlob,
                filePrefix, fileSuffix, fileDescription);
        return filePage;
    }

    /**
     * Creates a Collections container form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param collectionsTitle the Collections container title
     * @param fileDescription the collections description
     * @return the created Collections page
     */
    protected DocumentBasePage createCollections(DocumentBasePage currentPage, String collectionsTitle,
            String fileDescription) {
        DublinCoreCreationDocumentFormPage dublinCoreDocumentFormPage = currentPage.getContentTab()
                                                                                   .getDocumentCreatePage(
                                                                                           "Collections",
                                                                                           DublinCoreCreationDocumentFormPage.class);
        // Create File
        DocumentBasePage documentBasePage = dublinCoreDocumentFormPage.createDocument(collectionsTitle, fileDescription);
        return documentBasePage;
    }

    /**
     * Creates a Collection form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param collectionsTitle the Collections container title
     * @param fileDescription the collection description
     * @return the created Collections page
     */
    protected CollectionContentTabSubPage createCollection(DocumentBasePage currentPage, String collectionsTitle,
            String fileDescription) {
        CollectionCreationFormPage collectionCreationFormPage = currentPage.getContentTab().getDocumentCreatePage(
                "Collection", CollectionCreationFormPage.class);
        // Create File
        CollectionContentTabSubPage documentBasePage = collectionCreationFormPage.createDocument(collectionsTitle,
                fileDescription);
        return documentBasePage;
    }

    /**
     * Creates a temporary file and returns its absolute path.
     *
     * @param filePrefix the file prefix
     * @param fileSuffix the file suffix
     * @param fileContent the file content
     * @return the temporary file to upload path
     * @throws IOException if temporary file creation fails
     * @since 5.9.3
     */
    public static String getTmpFileToUploadPath(String filePrefix, String fileSuffix, String fileContent)
            throws IOException {
        // Create tmp file, deleted on exit
        File tmpFile = Framework.createTempFile(filePrefix, fileSuffix);
        tmpFile.deleteOnExit();
        FileUtils.writeFile(tmpFile, fileContent);
        assertTrue(tmpFile.exists());

        // Check file URI protocol
        assertEquals("file", tmpFile.toURI().toURL().getProtocol());

        // Return file absolute path
        return tmpFile.getAbsolutePath();
    }

    /**
     * Get the current document id stored in the javascript ctx.currentDocument variable of the current page.
     *
     * @return the current document id
     * @since 5.7
     */
    protected String getCurrentDocumentId() {
        return (String) ((JavascriptExecutor) driver).executeScript(String.format("return ctx.currentDocument;"));
    }

    /**
     * Creates a Note form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param noteTitle the note title
     * @param noteDescription the note description
     * @param defineNote true if the content of the note needs to be defined
     * @param noteContent the content of the note
     * @return the created note page.
     * @throws IOException
     * @since 5.9.4
     */
    protected NoteDocumentBasePage createNote(DocumentBasePage currentPage, String noteTitle, String noteDescription,
            boolean defineNote, String noteContent) throws IOException {
        // Get the Note creation form
        NoteCreationFormPage noteCreationPage = currentPage.getContentTab().getDocumentCreatePage("Note",
                NoteCreationFormPage.class);
        // Create a Note
        NoteDocumentBasePage notePage = noteCreationPage.createNoteDocument(noteTitle, noteDescription, defineNote,
                noteContent);
        return notePage;
    }

    /**
     * Do not run on windows with Firefox 26 (NXP-17848).
     *
     * @since 7.10
     */
    protected void doNotRunOnWindowsWithFF26() {
        String browser, browserVersion = null;
        try {
            browser = driver.getCapabilities().getBrowserName();
            browserVersion = driver.getCapabilities().getVersion();
            Float iBrowserVersion = Float.parseFloat(browserVersion);
            assumeFalse(SystemUtils.IS_OS_WINDOWS && browser.equals("firefox") && iBrowserVersion <= 28.0);
        } catch (NumberFormatException e) {
            log.warn("Could not parse browser version: " + browserVersion);
        }
    }

}

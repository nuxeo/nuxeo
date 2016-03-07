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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.functionaltests.drivers.ChromeDriverProvider;
import org.nuxeo.functionaltests.drivers.FirefoxDriverProvider;
import org.nuxeo.functionaltests.fragment.WebFragment;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.nuxeo.functionaltests.pages.NoteDocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.CollectionContentTabSubPage;
import org.nuxeo.functionaltests.proxy.ProxyManager;
import org.nuxeo.runtime.api.Framework;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.collect.ImmutableMap;

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

    public static final int POLLING_FREQUENCY_SECONDS = 1;

    /**
     * Page Load timeout in seconds.
     *
     * @since 5.9.2
     */
    public static final int PAGE_LOAD_TIME_OUT_SECONDS = 60;

    public static final int LOAD_TIMEOUT_SECONDS = 30;

    public static final int LOAD_SHORT_TIMEOUT_SECONDS = 2;

    public static final int AJAX_TIMEOUT_SECONDS = 10;

    public static final int AJAX_SHORT_TIMEOUT_SECONDS = 2;

    /**
     * @since 5.7
     * @deprecated since 8.2
     * @see ChromeDriverProvider
     */
    @Deprecated
    public static final String CHROME_DRIVER_DEFAULT_PATH_LINUX = ChromeDriverProvider.CHROME_DRIVER_DEFAULT_PATH_LINUX;

    /**
     * @since 5.7 "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" doesn't work
     * @deprecated since 8.2
     * @see ChromeDriverProvider
     */
    @Deprecated
    public static final String CHROME_DRIVER_DEFAULT_PATH_MAC = ChromeDriverProvider.CHROME_DRIVER_DEFAULT_PATH_MAC;

    /**
     * @since 5.7
     * @deprecated since 8.2
     * @see ChromeDriverProvider
     */
    @Deprecated
    public static final String CHROME_DRIVER_DEFAULT_PATH_WINVISTA = ChromeDriverProvider.CHROME_DRIVER_DEFAULT_PATH_WINVISTA;

    /**
     * @since 5.7
     * @deprecated since 8.2
     * @see ChromeDriverProvider
     */
    @Deprecated
    public static final String CHROME_DRIVER_DEFAULT_PATH_WINXP = ChromeDriverProvider.CHROME_DRIVER_DEFAULT_PATH_WINXP;

    /**
     * @since 5.7
     * @deprecated since 8.2
     * @see ChromeDriverProvider
     */
    @Deprecated
    public static final String CHROME_DRIVER_DEFAULT_EXECUTABLE_NAME = ChromeDriverProvider.CHROME_DRIVER_DEFAULT_EXECUTABLE_NAME;

    /**
     * @since 5.7
     * @deprecated since 8.2
     * @see ChromeDriverProvider
     */
    @Deprecated
    public static final String CHROME_DRIVER_WINDOWS_EXECUTABLE_NAME = ChromeDriverProvider.CHROME_DRIVER_WINDOWS_EXECUTABLE_NAME;

    /**
     * @deprecated since 8.2
     * @see ChromeDriverProvider
     */
    @Deprecated
    public static final String SYSPROP_CHROME_DRIVER_PATH = ChromeDriverProvider.SYSPROP_CHROME_DRIVER_PATH;

    static final Log log = LogFactory.getLog(AbstractTest.class);

    public static final String NUXEO_URL = System.getProperty("nuxeoURL", "http://localhost:8080/nuxeo").replaceAll(
            "/$", "");

    public static RemoteWebDriver driver;

    protected static ProxyManager proxyManager;

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
        proxyManager = new ProxyManager();
        Proxy proxy = proxyManager.startProxy();
        if (proxy != null) {
            proxy.setNoProxy("");
        }
        DesiredCapabilities dc = DesiredCapabilities.firefox();
        dc.setCapability(CapabilityType.PROXY, proxy);
        driver = new FirefoxDriverProvider().init(dc);
    }

    protected static void initChromeDriver() throws Exception {
        proxyManager = new ProxyManager();
        Proxy proxy = proxyManager.startProxy();
        if (proxy != null) {
            proxy.setNoProxy("");
        }
        DesiredCapabilities dc = DesiredCapabilities.chrome();
        dc.setCapability(CapabilityType.PROXY, proxy);
        driver = new ChromeDriverProvider().init(dc);
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

        try {
            proxyManager.stopProxy();
            proxyManager = null;
        } catch (Exception e) {
            log.error("Could not stop proxy: " + e.getMessage());
        }
    }

    public static <T> T get(String url, Class<T> pageClassToProxy) {
        if (driver != null) {
            new JavaScriptErrorCollector(driver).checkForErrors();
        }
        driver.get(url);
        return asPage(pageClassToProxy);
    }

    public static void open(String url) {
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
            return wait.until(aPage -> {
                String notLoaded = anyElementNotLoaded(elements, fieldNames);
                if (notLoaded == null) {
                    return aPage;
                } else {
                    return null;
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
        return login(ADMINISTRATOR, ADMINISTRATOR);
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
     * @param username the username
     * @param password the password
     */
    public LoginPage loginInvalid(String username, String password) {
        return getLoginPage().login(username, password, LoginPage.class);
    }

    /**
     * Init the repository with a test Workspace form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @return the created Workspace page
     * @throws Exception if initializing repository fails
     */
    @Deprecated
    protected DocumentBasePage initRepository(DocumentBasePage currentPage) throws Exception {
        return createWorkspace(currentPage, "Test Workspace", "Test Workspace for my dear WebDriver.");
    }

    /**
     * Cleans the repository (delete the test Workspace) from the {@code currentPage}.
     *
     * @param currentPage the current page
     * @throws Exception if cleaning repository fails
     */
    @Deprecated
    protected void cleanRepository(DocumentBasePage currentPage) throws Exception {
        deleteWorkspace(currentPage, "Test Workspace");
    }

    /**
     * Creates a Workspace from the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param workspaceTitle the workspace title
     * @param workspaceDescription the workspace description
     * @return the created Workspace page
     * @deprecated since 8.2: use {@link DocumentBasePage#createWorkspace(String, String)} instead.
     */
    @Deprecated
    protected DocumentBasePage createWorkspace(DocumentBasePage currentPage, String workspaceTitle,
            String workspaceDescription) {
        return currentPage.createWorkspace(workspaceTitle, workspaceDescription);
    }

    /**
     * Deletes the Workspace with title {@code workspaceTitle} from the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param workspaceTitle the workspace title
     * @deprecated since 8.2: use {@link DocumentBasePage#deleteWorkspace(String)} instead.
     */
    @Deprecated
    protected void deleteWorkspace(DocumentBasePage currentPage, String workspaceTitle) {
        currentPage.deleteWorkspace(workspaceTitle);
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
     * @deprecated since 8.2: use {@link DocumentBasePage#createFile(String, String, boolean, String, String, String)}
     *             instead.
     */
    @Deprecated
    protected FileDocumentBasePage createFile(DocumentBasePage currentPage, String fileTitle, String fileDescription,
            boolean uploadBlob, String filePrefix, String fileSuffix, String fileContent) throws IOException {
        return currentPage.createFile(fileTitle, fileDescription, uploadBlob, filePrefix, fileSuffix, fileContent);
    }

    /**
     * Creates a Collections container form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param collectionsTitle the Collections container title
     * @param fileDescription the collections description
     * @return the created Collections page
     * @deprecated since 8.2: use {@link DocumentBasePage#createCollections(String, String)} instead.
     */
    @Deprecated
    protected DocumentBasePage createCollections(DocumentBasePage currentPage, String collectionsTitle,
            String fileDescription) {
        return currentPage.createCollections(collectionsTitle, fileDescription);
    }

    /**
     * Creates a Collection form the {@code currentPage}.
     *
     * @param currentPage the current page
     * @param collectionsTitle the Collections container title
     * @param fileDescription the collection description
     * @return the created Collections page
     * @deprecated since 8.2: use {@link DocumentBasePage#createCollection(String, String)} instead.
     */
    @Deprecated
    protected CollectionContentTabSubPage createCollection(DocumentBasePage currentPage, String collectionsTitle,
            String fileDescription) {
        return currentPage.createCollection(collectionsTitle, fileDescription);
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
        return (String) driver.executeScript("return ctx.currentDocument;");
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
     * @deprecated since 8.2: use {@link DocumentBasePage#createNote(String, String, boolean, String)} instead.
     */
    @Deprecated
    protected NoteDocumentBasePage createNote(DocumentBasePage currentPage, String noteTitle, String noteDescription,
            boolean defineNote, String noteContent) throws IOException {
        return currentPage.createNote(noteTitle, noteDescription, defineNote, noteContent);
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

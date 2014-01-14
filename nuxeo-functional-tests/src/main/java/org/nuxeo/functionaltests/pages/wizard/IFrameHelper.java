package org.nuxeo.functionaltests.pages.wizard;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * Wizard and Connect use frames and callback pages to communicate. So focusing
 * the right frame can be tricky because, for example we never want to do any
 * test on the callback pages.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class IFrameHelper {

    protected static final Log log = LogFactory.getLog(IFrameHelper.class);

    public static final String CONNECT_IFRAME_URL_PATTERN = "/site/connect";

    public static final String CALLBACK_URL_PATTERN = "ConnectCallback";

    public static final String CONNECT_FRAME_NAME = "connectForm";

    private static void switchToIFrame(final WebDriver driver,
            final WebElement iframe) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS,
                TimeUnit.MILLISECONDS).ignoring(NoSuchFrameException.class,
                StaleElementReferenceException.class);
        wait.until(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                if (iframe == null) {
                    return driver.switchTo().defaultContent() != null;
                } else {
                    return driver.switchTo().frame(iframe) != null;
                }
            };
        });
    }

    public static boolean focusOnConnectFrame(WebDriver driver) {
        if (!driver.getCurrentUrl().contains(CONNECT_IFRAME_URL_PATTERN)) {
            try {
                WebElement connectFormIFrame = Locator.findElementWithTimeout(By.id(CONNECT_FRAME_NAME));
                switchToIFrame(driver, connectFormIFrame);
            } catch (TimeoutException e) {
                log.error("Unable to find IFrame on page "
                        + driver.getCurrentUrl());
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean focusOnWizardPage(WebDriver driver) {

        Locator.waitUntilURLNotContain(CALLBACK_URL_PATTERN);

        // if we're coming from an iframe, driver.getCurrentUrl() can be empty
        // switch back to main frame without testing the URL
        try {
            switchToIFrame(driver, null);
            return true;
        } catch (TimeoutException e) {
            log.error("Unable to find top windows on page "
                    + driver.getCurrentUrl());
            return false;
        }
    }
}

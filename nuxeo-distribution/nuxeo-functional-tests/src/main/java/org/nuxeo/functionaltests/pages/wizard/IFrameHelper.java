package org.nuxeo.functionaltests.pages.wizard;

import org.openqa.selenium.WebDriver;

/**
 * Wizard and Connect use frames and callback pages to communicate.
 * So focusing the right frame can be tricky because, for example we never want to do any test on the callback pages.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class IFrameHelper {

    public static final String CONNECT_IFRAME_URL_PATTERN = "/site/connect";
    public static final String CALLBACK_URL_PATTERN = "ConnectCallback";
    public static final String CONNECT_FRAME_NAME = "connectForm";
    public static final int NBTRY = 10;

    public static boolean focusOnConnectFrame(WebDriver driver) {
            return focusOnConnectFrame(driver, NBTRY);
    }

    protected static void wait(int nbSeconds) {
        try {
            Thread.sleep(nbSeconds * 1000);
        } catch (InterruptedException e) {
        }
    }

    private static boolean focusOnConnectFrame(WebDriver driver, int nbTry) {

        if (driver.getCurrentUrl().contains(CALLBACK_URL_PATTERN) && nbTry >0) {
            // we must wait for the callback page to execute and do the redirect
            wait(1);
            return focusOnConnectFrame(driver, nbTry-1);
        }

        if (!driver.getCurrentUrl().contains(CONNECT_IFRAME_URL_PATTERN)) {
            try {
                driver.switchTo().frame(CONNECT_FRAME_NAME);
                return true;
            } catch (Throwable e) {
                if (nbTry>0) {
                    System.out.println("Retry to find IFrame on page " + driver.getCurrentUrl());
                    wait(2);
                    return focusOnConnectFrame(driver, nbTry-1);
                } else {
                    System.out.println("Unable to find IFrame on page " + driver.getCurrentUrl());
                    System.out.println(driver.getPageSource());
                }
            }
        }
        return false;
    }

    public static boolean focusOnWizardPage(WebDriver driver) {
        return focusOnWizardPage(driver, NBTRY);
    }

    private static boolean focusOnWizardPage(WebDriver driver, int nbTry) {

        if (driver.getCurrentUrl().contains(CALLBACK_URL_PATTERN) && nbTry >0) {
            // we must wait for the callback page to execute and do the redirect
            wait(2);
            return focusOnWizardPage(driver, nbTry-1);
        }

        // if we're coming from an iframe, driver.getCurrentUrl() can be empty
        // switch back to main frame without testing the URL
        try {
            driver.switchTo().defaultContent();
            return true;
        } catch (Throwable e) {
            if (nbTry>0) {
                System.out.println("Retry to find top windows on page " + driver.getCurrentUrl());
                wait(1);
                return focusOnWizardPage(driver, nbTry-1);
            } else {
                System.out.println("Unable to find top windows on page " + driver.getCurrentUrl());
            }
        }
        return false;
    }
}

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


    public static boolean focusOnConnectFrame(WebDriver driver) {
            return focusOnConnectFrame(driver, 3);
    }

    private static boolean focusOnConnectFrame(WebDriver driver, int nbTry) {

        if (driver.getCurrentUrl().contains(CALLBACK_URL_PATTERN) && nbTry >0) {
            // we must wait for the callback page to execute and do the redirect
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return focusOnConnectFrame(driver, nbTry-1);
        }

        if (!driver.getCurrentUrl().contains(CONNECT_IFRAME_URL_PATTERN)) {
            try {
                driver.switchTo().frame(CONNECT_FRAME_NAME);
                return true;
            } catch (Throwable e) {
                System.out.println("Unable to find IFrame on page " + driver.getCurrentUrl());
            }
        }
        return false;
    }

    public static boolean focusOnWizardPage(WebDriver driver) {
        return focusOnWizardPage(driver, 3);
    }

    private static boolean focusOnWizardPage(WebDriver driver, int nbTry) {

        if (driver.getCurrentUrl().contains(CALLBACK_URL_PATTERN) && nbTry >0) {
            // we must wait for the callback page to execute and do the redirect
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return focusOnWizardPage(driver, nbTry-1);
        }

        if (driver.getCurrentUrl().contains(CONNECT_IFRAME_URL_PATTERN)) {
            try {
                driver.switchTo().defaultContent();
                return true;
            } catch (Throwable e) {
                System.out.println("Unable to top windows on page " + driver.getCurrentUrl());
            }
        }
        return false;
    }

}

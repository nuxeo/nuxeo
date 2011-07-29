package org.nuxeo.functionaltests.pages.admincenter;

import org.openqa.selenium.WebDriver;

/**
 * Wizard and Connect use frames and callback pages to communicate.
 * So focusing the right frame can be tricky because, for example we never want to do any test on the callback pages.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class IFrameHelper {

    public static final String WE_IFRAME_URL_PATTERN = "/site/connectClient";

    public static boolean focusOnWEIFrame(WebDriver driver) {
            return focusOnWEIFrame(driver, 3);
    }

    private static boolean focusOnWEIFrame(WebDriver driver, int nbTry) {

        if (!driver.getCurrentUrl().contains(WE_IFRAME_URL_PATTERN)) {
            if (nbTry >0) {
                try {
                    Thread.sleep(1000);
                    driver.switchTo().frame(0);
                } catch (InterruptedException e) {
                }
                return focusOnWEIFrame(driver, nbTry-1);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

}

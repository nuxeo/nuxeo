/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.functionaltests.pages.ErrorPage.ERROR_OCCURED_TITLE;
import static org.nuxeo.functionaltests.pages.ErrorPage.ERROR_OCCURED_MESSAGE;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.ErrorPage;
import org.openqa.selenium.By;

/**
 * Facelet errors tests.
 */
public class ITFaceletErrorTest extends AbstractTest {

    private static final String GO_TO_ERROR_PAGE = "Go to error page";

    private static final String ERROR_MESSAGE_XPATH = "//span[@style='color:red;font-weight:bold;']";

    // the tested server is not in dev mode so we don't have the facelet path in the error message
    private static final String FACELET_NOT_FOUND_MESSAGE = "ERROR: facelet not found";

    @Test
    public void testUiIncludeNotFound() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Error ui:include not found"));
        assertEquals(FACELET_NOT_FOUND_MESSAGE, driver.findElementByXPath(ERROR_MESSAGE_XPATH).getText());
    }

    @Test
    public void testUiIncludeRelativeNotFound() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Error ui:include relative not found"));
        assertEquals(FACELET_NOT_FOUND_MESSAGE, driver.findElementByXPath(ERROR_MESSAGE_XPATH).getText());
    }

    @Test
    public void testUiDecorateNotFound() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Error ui:decorate not found"));
        assertEquals(FACELET_NOT_FOUND_MESSAGE, driver.findElementByXPath(ERROR_MESSAGE_XPATH).getText());
    }

    @Test
    public void testUiCompositionNotFound() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Error ui:composition not found"));
        assertEquals(FACELET_NOT_FOUND_MESSAGE, driver.findElementByXPath(ERROR_MESSAGE_XPATH).getText());
    }

    @Test
    public void testUiIncludeInvalid() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Error ui:include invalid"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testUiIncludeRelativeInvalid() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Error ui:include relative invalid"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

}

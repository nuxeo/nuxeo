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

import static org.nuxeo.functionaltests.pages.ErrorPage.ERROR_OCCURED_MESSAGE;
import static org.nuxeo.functionaltests.pages.ErrorPage.ERROR_OCCURED_TITLE;
import static org.nuxeo.functionaltests.pages.ErrorPage.NO_SUFFICIENT_RIGHTS_MESSAGE;
import static org.nuxeo.functionaltests.pages.ErrorPage.NO_SUFFICIENT_RIGHTS_TITLE;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.ErrorPage;
import org.openqa.selenium.By;

/**
 * Error pages tests.
 */
public class ITErrorPageTest extends AbstractTest {

    private static final String GO_TO_ERROR_PAGE = "Go to error page";

    @Test
    public void testInvalidHTMLError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Error invalid html"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testJSFChckedError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("JSF checked error"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testJSFUnchckedError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("JSF unchecked error"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testSeamGetterCheckedError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam getter checked error"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testSeamGetterUncheckedError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam getter unchecked error"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testSeamGetterSecurityError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam getter security error"));
        asPage(ErrorPage.class).checkErrorPage(NO_SUFFICIENT_RIGHTS_TITLE, NO_SUFFICIENT_RIGHTS_MESSAGE, true, false,
                false, "");
    }

    @Test
    public void testSeamFactoryCheckedError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam factory checked error"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testSeamFactoryUncheckedError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam factory unchecked error"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testSeamFactorySecurityError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam factory security error"));
        asPage(ErrorPage.class).checkErrorPage(NO_SUFFICIENT_RIGHTS_TITLE, NO_SUFFICIENT_RIGHTS_MESSAGE, true, false,
                false, "");
    }

    @Test
    public void testSeamActionUncheckedError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam action unchecked error"));
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testSeamActionSecurityError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam action security error"));
        asPage(ErrorPage.class).checkErrorPage(NO_SUFFICIENT_RIGHTS_TITLE, NO_SUFFICIENT_RIGHTS_MESSAGE, true, false,
                false, "");
    }

}

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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;

/**
 * Webengine errors tests.
 */
public class ITWebengineErrorTest extends AbstractTest {

    private static final CharSequence WEBENGINE_ERROR_MESSAGE = "WEBENGINE HANDLED ERROR:";

    @Test
    public void testNuxeoException() throws Exception {
        driver.get(NUXEO_URL + "/site/error");
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Nuxeo Exception"));
        String bodyText = AbstractPage.findElementAndWaitUntilEnabled(By.tagName("body")).getText();
        assertTrue(bodyText.contains(WEBENGINE_ERROR_MESSAGE));
        assertTrue(bodyText.contains("org.nuxeo.ecm.core.api.NuxeoException: Nuxeo exception"));
    }

    @Test
    public void testCheckedError() throws Exception {
        driver.get(NUXEO_URL + "/site/error");
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Checked Error"));
        String bodyText = AbstractPage.findElementAndWaitUntilEnabled(By.tagName("body")).getText();
        assertTrue(bodyText.contains(WEBENGINE_ERROR_MESSAGE));
        assertTrue(bodyText.contains("java.lang.Exception: CheckedError in webengine"));
    }

    @Test
    public void testUncheckedError() throws Exception {
        driver.get(NUXEO_URL + "/site/error");
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Unchecked Error"));
        String bodyText = AbstractPage.findElementAndWaitUntilEnabled(By.tagName("body")).getText();
        assertTrue(bodyText.contains(WEBENGINE_ERROR_MESSAGE));
        assertTrue(bodyText.contains("java.lang.NullPointerException: UncheckedError in webengine"));
    }

    @Test
    public void testSecurityError() throws Exception {
        driver.get(NUXEO_URL + "/site/error");
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Security Error"));
        String bodyText = AbstractPage.findElementAndWaitUntilEnabled(By.tagName("body")).getText();
        assertTrue(bodyText.contains(WEBENGINE_ERROR_MESSAGE));
        assertTrue(bodyText.contains("org.nuxeo.ecm.core.api.DocumentSecurityException: Security error in webengine"));
    }
}

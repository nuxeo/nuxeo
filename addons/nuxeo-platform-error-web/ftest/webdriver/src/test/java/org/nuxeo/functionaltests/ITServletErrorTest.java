/*
 * (C) Copyright 2017 Nuxeo(http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.functionaltests.pages.ErrorPage.ERROR_OCCURED_MESSAGE;
import static org.nuxeo.functionaltests.pages.ErrorPage.ERROR_OCCURED_TITLE;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.ErrorPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Servlet errors tests.
 */
public class ITServletErrorTest extends AbstractTest {

    @Test
    public void testNoException() throws Exception {
        driver.get(NUXEO_URL + "/nxerror/nothingspecial");
        WebElement element = driver.findElement(By.tagName("p"));
        assertNotNull(element);
        assertEquals("ok", element.getText());
    }

    @Test
    public void testNullPointerException() throws Exception {
        driver.get(NUXEO_URL + "/nxerror/npe"); // causes a NullPointerException
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testNullPointerExceptionAfterGetOutputStream() throws Exception {
        driver.get(NUXEO_URL + "/nxerror/gos/npe"); // getOutputStream then NullPointerException
        asPage(ErrorPage.class).checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

}

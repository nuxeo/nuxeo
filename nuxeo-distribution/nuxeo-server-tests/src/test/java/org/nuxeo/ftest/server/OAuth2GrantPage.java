/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;

/**
 * Representation of the {@code oauth2Grant.jsp} page for WebDriver.
 *
 * @since 9.2
 */
public class OAuth2GrantPage extends AbstractPage {

    @Required
    @FindBy(tagName = "form")
    WebElement form;

    @Required
    @FindBy(name = "response_type")
    WebElement responseType;

    @Required
    @FindBy(name = "client_id")
    WebElement clientId;

    @FindBy(name = "redirect_uri")
    WebElement redirectURI;

    @Required
    @FindBy(name = "deny_access")
    WebElement denyAccess;

    @Required
    @FindBy(name = "grant_access")
    WebElement grantAccess;

    public OAuth2GrantPage(WebDriver driver) {
        super(driver);
    }

    public void checkClientName(String name) {
        assertTrue(form.getText().contains(name));
    }

    public void checkResponseType(String expected) {
        assertEquals(expected, responseType.getAttribute("value"));
    }

    public void checkClientId(String expected) {
        assertEquals(expected, clientId.getAttribute("value"));
    }

    public void checkExtraParameter(String name, String expected) {
        assertEquals(expected, driver.findElement(By.name(name)).getAttribute("value"));
    }

    public void checkFieldCount(int count) {
        assertEquals(count, driver.findElements(By.tagName("input")).size());
    }

    public void setFieldValue(String name, String value) {
        ((RemoteWebDriver) driver).executeScript(
                String.format("document.getElementsByName('%s')[0].value = '%s' ;", name, value));
    }

    public void removeField(String name) {
        ((RemoteWebDriver) driver).executeScript(String.format("document.getElementsByName('%s')[0].remove() ;", name));
    }

    public void deny() {
        denyAccess.click();
    }

    public void grant() {
        grantAccess.click();
    }

}

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
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

    protected final static String AUTHORIZATION_KEY_INPUT_NAME = "authorization_key";

    @Required
    @FindBy(tagName = "form")
    WebElement form;

    @Required
    @FindBy(name = AUTHORIZATION_KEY_INPUT_NAME)
    WebElement authorizationKey;

    @Required
    @FindBy(name = "state")
    WebElement state;

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

    public void checkAuthorizationKey() {
        assertNotNull(authorizationKey.getAttribute("value"));
    }

    public void checkState(String expectedState) {
        assertEquals(expectedState, state.getAttribute("value"));
    }

    public void setAuthorizationKey(RemoteWebDriver driver, String key) {
        driver.executeScript(
                String.format("document.getElementsByName('%s')[0].value = '%s' ;", AUTHORIZATION_KEY_INPUT_NAME, key));
    }

    public void deny() {
        denyAccess.click();
    }

    public void grant() {
        grantAccess.click();
    }

}

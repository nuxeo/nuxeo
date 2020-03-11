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

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Representation of the {@code oauth2error.jsp} page for WebDriver.
 *
 * @since 9.2
 */
public class OAuth2ErrorPage extends AbstractPage {

    @Required
    @FindBy(tagName = "h1")
    WebElement title;

    @Required
    @FindBy(tagName = "code")
    WebElement description;

    public OAuth2ErrorPage(WebDriver driver) {
        super(driver);
    }

    public void checkTitle(String expectedTitle) {
        assertEquals(expectedTitle, title.getText());
    }

    public void checkDescription(String expectedDescription) {
        assertEquals(expectedDescription, description.getText());
    }

}

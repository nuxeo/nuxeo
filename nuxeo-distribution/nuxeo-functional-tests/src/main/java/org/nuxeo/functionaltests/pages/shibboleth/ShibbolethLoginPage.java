/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.functionaltests.pages.shibboleth;

import org.nuxeo.functionaltests.JavaScriptErrorCollector;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 9.10
 */
public class ShibbolethLoginPage extends LoginPage {

    @Required
    @FindBy(name = "j_username")
    protected WebElement shibUsernameInputTextBox;

    @Required
    @FindBy(name = "j_password")
    protected WebElement shibPasswordInputTextBox;

    @Required
    @FindBy(className = "form-button")
    protected WebElement submitButton;

    public ShibbolethLoginPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void login(String username, String password) {
        JavaScriptErrorCollector.from(driver)
                                .ignore(JavaScriptErrorCollector.JavaScriptErrorIgnoreRule.startsWith(
                                        "SyntaxError: expected expression"))
                                .checkForErrors();
        shibUsernameInputTextBox.sendKeys(username);
        shibPasswordInputTextBox.sendKeys(password);
        submitButton.click();
    }

}

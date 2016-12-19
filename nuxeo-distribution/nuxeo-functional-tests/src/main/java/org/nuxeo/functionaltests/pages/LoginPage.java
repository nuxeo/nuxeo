/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo default login page.
 */
public class LoginPage extends AbstractPage {

    public static final String FEEDBACK_MESSAGE_DIV_XPATH = "//div[contains(@class,'feedbackMessage')]";

    public static final String LOGIN_DIV_XPATH = "//div[@class='login']";

    @Required
    @FindBy(id = "username")
    WebElement usernameInputTextBox;

    @Required
    @FindBy(id = "password")
    WebElement passwordInputTextBox;

    @Required
    @FindBy(name = "Submit")
    WebElement submitButton;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Fills in the login form with the username, password and language.
     *
     * @param username the username
     * @param password the password
     * @param language value of one of the options in the language select box. For example, English (United States)
     * @deprecated since 9.1 not used anymore, use {@link #login(String, String)} insted.
     */
    @Deprecated
    public void login(String username, String password, String language) {
        login(username, password);
    }

    /**
     * Fills in the login form with the username and password. Uses the default language.
     *
     * @param username the username
     * @param password the password
     */
    public void login(String username, String password) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        usernameInputTextBox.sendKeys(username);
        passwordInputTextBox.sendKeys(password);
        jsExecutor.executeScript("document.getElementById('username').blur();return true;");
        jsExecutor.executeScript("document.getElementById('password').blur();return true;");

        submitButton.click();
    }

    /**
     * Logs in and returns the next page.
     *
     * @param username the username
     * @param password the password
     * @param pageClassToProxy the next page's class
     * @return the next page
     */
    public <T> T login(String username, String password, Class<T> pageClassToProxy) {
        try {
            login(username, password);
            return asPage(pageClassToProxy);
        } catch (NoSuchElementException | TimeoutException exc) {
            try {
                // Try once again because of problem described in NXP-12835.
                // find the real cause of NXP-12835 and remove second login
                // attempt
                if (hasElement(By.xpath(LOGIN_DIV_XPATH))) {
                    login(username, password);
                    return asPage(pageClassToProxy);
                } else {
                    throw exc;
                }
            } catch (NoSuchElementException e) {
                if (hasElement(By.xpath(LOGIN_DIV_XPATH))) {
                    // Means we are still on login page.
                    if (hasElement(By.xpath(FEEDBACK_MESSAGE_DIV_XPATH))) {
                        throw new NoSuchElementException("Login failed. Application said : "
                                + driver.findElement(By.xpath(FEEDBACK_MESSAGE_DIV_XPATH)).getText(), e);
                    } else {
                        throw new NoSuchElementException("Login failed", e);
                    }
                } else {
                    throw e;
                }
            }
        }
    }
}

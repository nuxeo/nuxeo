/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN
 *     Florent Guillaume
 */
package org.nuxeo.functionaltests.pages;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Nuxeo default login page.
 */
public class LoginPage extends AbstractPage {

    private static final Log log = LogFactory.getLog(LoginPage.class);

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

    /**
     * removed from login page since 5.6
     */
    @Deprecated
    @FindBy(id = "language")
    WebElement languageSelectBox;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Fills in the login form with the username, password and language.
     *
     * @param username the username
     * @param password the password
     * @param language value of one of the options in the language select box.
     *            For example, English (United States)
     */
    public void login(String username, String password, String language) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        usernameInputTextBox.sendKeys(username);
        passwordInputTextBox.sendKeys(password);
        jsExecutor.executeScript("document.getElementById('username').blur();return true;");
        jsExecutor.executeScript("document.getElementById('password').blur();return true;");

        if (language != null) {
            Select languageSelect = new Select(languageSelectBox);

            List<WebElement> list = languageSelect.getOptions();
            for (WebElement webElement : list) {
                if (language.trim().equals(webElement.getText().trim())) {
                    languageSelect.selectByVisibleText(webElement.getText());
                    break;
                }
            }
        }
        submitButton.click();
    }

    /**
     * Fills in the login form with the username and password. Uses the default
     * language.
     *
     * @param username the username
     * @param password the password
     */
    public void login(String username, String password) {
        login(username, password, (String) null);
    }

    /**
     * Logs in and returns the next page.
     *
     * @param username the username
     * @param password the password
     * @param pageClassToProxy the next page's class
     * @return the next page
     */
    public <T> T login(String username, String password,
            Class<T> pageClassToProxy) {
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
                        throw new NoSuchElementException(
                                "Login failed. Application said : "
                                        + driver.findElement(
                                                By.xpath(FEEDBACK_MESSAGE_DIV_XPATH)).getText(),
                                e);
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

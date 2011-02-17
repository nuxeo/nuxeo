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

import static junit.framework.Assert.assertNotNull;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * Nuxeo default login page.
 */
public class LoginPage extends AbstractPage {

    @FindBy(id = "username")
    WebElement usernameInputTextBox;

    @FindBy(id = "password")
    WebElement passwordInputTextBox;

    @FindBy(name = "Submit")
    WebElement submitButton;

    @FindBy(id = "language")
    WebElement languageSelectBox;

    public LoginPage(WebDriver driver) {
        super(driver);
        assertNotNull(driver.findElement(By.id("username")));
        assertNotNull(driver.findElement(By.id("password")));
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

        usernameInputTextBox.sendKeys(username);
        passwordInputTextBox.sendKeys(password);

        if (language != null) {
            List<WebElement> list = languageSelectBox.findElements(By.tagName("option"));
            for (WebElement webElement : list) {
                if (language.trim().equals(webElement.getText().trim())) {
                    webElement.setSelected();
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
        login(username, password, null);
    }

    /**
     * Logs in and returns the next page.
     *
     * @param username the username
     * @param password the password
     * @param page the next page's class
     * @return the next page
     */
    public <T> T loginTo(String username, String password, Class<T> page) {
        login(username, password);
        return PageFactory.initElements(driver, page);
    }

}

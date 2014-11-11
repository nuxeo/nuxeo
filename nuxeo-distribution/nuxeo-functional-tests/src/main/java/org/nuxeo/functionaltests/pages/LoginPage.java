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

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo default login page.
 */
public class LoginPage extends AbstractPage {

    @Required
    @FindBy(id = "username")
    WebElement usernameInputTextBox;

    @Required
    @FindBy(id = "password")
    WebElement passwordInputTextBox;

    @Required
    @FindBy(name = "Submit")
    WebElement submitButton;

    @Required
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
        login(username, password);
        return asPage(pageClassToProxy);
    }

}

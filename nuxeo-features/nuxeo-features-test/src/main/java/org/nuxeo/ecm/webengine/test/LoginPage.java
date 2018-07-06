/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.test;

import org.nuxeo.runtime.test.runner.web.WebPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 9.3
 */
@Deprecated
public class LoginPage extends WebPage {

    @FindBy(how = How.ID, using = "username")
    protected WebElement inputUsername;

    @FindBy(how = How.ID, using = "password")
    protected WebElement inputPassword;

    @FindBy(how = How.NAME, using = "nuxeo_login")
    protected WebElement login;

    @FindBy(how = How.ID, using = "logout")
    protected WebElement logout;

    @FindBy(how = How.ID, using = "logstate")
    protected WebElement logstate;

    public void login(String username, String password) {
        inputUsername.clear();
        inputUsername.sendKeys(username);
        inputPassword.clear();
        inputPassword.sendKeys(password);
        login.click();
    }

    public void ensureLogin(String username, String password) {
        login(username, password);
        isAuthenticated(5);
    }

    public void logout() {
        logout.click();
    }

    public void ensureLogout() {
        logout();
        isNotAuthenticated(5);
    }

    public boolean isAuthenticated(int timeoutInSeconds) {
        try {
            findElement(By.id("logout"), timeoutInSeconds);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isNotAuthenticated(int timeoutInSeconds) {
        try {
            findElement(By.id("login"), timeoutInSeconds);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isAuthenticated() {
        try {
            driver.findElement(By.id("logout"));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}

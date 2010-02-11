/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.webengine.test.web.pages;

import org.nuxeo.ecm.platform.test.web.pages.AbstractPage;
import org.nuxeo.ecm.platform.test.web.pages.WebPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WebEngineHomePage extends AbstractPage implements PageHeader,
        WebPage {

    private String path = "";

    public WebEngineHomePage(WebDriver driver, String host, String port,
            String path) {
        super(driver, host, port);
        this.path = path;
    }

    /**
     * This assumes to be on a Jetty Standalone WebEngine.
     */
    public WebEngineHomePage(WebDriver driver, String host, String port) {
        super(driver, host, port);
    }

    /**
     * Logs in with the specified login/password.
     */
    public void loginAs(String login, String password) {
        enterTextWithId(login, "username");
        enterTextWithId(password, "password");
        WebElement loginButton = getDriver().findElement(
                By.xpath("//input[@id='login']"));
        loginButton.click();
    }

    /**
     * Checks if the current session is logged in.
     */
    public boolean isLogged() {
        try {
            getDriver().findElement(By.id("logout"));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Loads or reload the WebEngine homepage.
     */
    public void reload() {
        visit(path);
    }

    /**
     * Logs out
     */
    public PageHeader logout() {
        WebElement logout;
        try {
            logout = getDriver().findElement(By.id("logout"));
            logout.click();
        } catch (NoSuchElementException e) {
            // ignore
        }
        return this;
    }

    /**
     * Checks if a given application is present on the webengine homepage
     */
    public boolean hasApplication(String appName) {
        return containsLink(appName);
    }

    /**
     * Visit the given application and returns its page
     *
     * @param appName The app to visit
     * @param serviceClass The class to use to visit the new app
     * @return An AbstractPage representing the application
     */
    public <T extends AbstractPage> T goToApplication(String appName,
            Class<T> serviceClass) throws InstantiationException,
            IllegalAccessException {
        return super.visit(appName, serviceClass);
    }

}

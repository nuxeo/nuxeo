package org.nuxeo.ecm.webengine.test.web.pages;

import org.nuxeo.runtime.api.Framework;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WebEngineHomePage extends AbstractPage implements PageHeader,
        WebPage {

    private String path = "";

    public WebEngineHomePage(WebDriver driver, String host, String port, String path) {
        super(driver, host, port);
        this.path = path;
    }

    /**
     * This assumes to be on a Jetty Standalone WebEngine
     * @param driver
     * @param host
     * @param port
     */
    public WebEngineHomePage(WebDriver driver, String host, String port) {
        super(driver, host, port);
    }

    /**
     * Logs in with the specified login/password
     * @param login : the login
     * @param password : the password
     */
    public void loginAs(String login, String password) {
        enterTextWithId(login, "username");
        enterTextWithId(password, "password");
        WebElement loginButton = getDriver().findElement(
                By.xpath("//input[@id='login']"));
        loginButton.click();
    }

    /**
     * Checks if the current session is logged
     * @return
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
     * Loads or reload the WebEngine homepage
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
        }
        return this;
    }


    /**
     * Checks if a given application is present on the webengine homepage
     * @param appName
     * @return
     */
    public Boolean hasApplication(String appName) {
        return containsLink(appName);
    }


    /**
     * Visit the given application and returns its page
     * @param appName The app to visit
     * @param serviceClass The class to use to visit the new app
     * @return An AbstractPage representing the application
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public <T extends AbstractPage> T goToApplication(String appName,
            Class<T> serviceClass) throws InstantiationException,
            IllegalAccessException {

        return super.visit(appName, serviceClass);
    }

}

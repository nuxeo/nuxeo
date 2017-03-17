package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class NoUIPage extends AbstractPage {

    public static final String WELCOME_MESSAGE_XPATH = "//div[@class='welcome']/h2";

    @Required
    @FindBy(xpath = WELCOME_MESSAGE_XPATH)
    WebElement welcomeMessage;

    public NoUIPage(WebDriver driver) {
        super(driver);
    }
}

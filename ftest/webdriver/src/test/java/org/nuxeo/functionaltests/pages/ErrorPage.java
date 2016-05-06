/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages;

import static org.junit.Assert.assertEquals;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.3
 */
public class ErrorPage extends AbstractPage {



    @FindBy(tagName = "h1")
    WebElement title;

    @FindBy(tagName = "p")
    WebElement message;

    @FindBy(linkText = "Back to Home Page")
    WebElement backToHomeLink;

    @FindBy(linkText = "Log Out")
    WebElement logOutLink;

    @FindBy(linkText = "Show Error Stacktrace")
    WebElement showStackTraceLink;

    @FindBy(linkText = "Show Error Context Dump")
    WebElement showContextDumpLink;

    public ErrorPage(WebDriver driver) {
        super(driver);
    }

    public void checkTitle(String expectedTitle) {
        assertEquals(expectedTitle, title.getText());
    }

    public void checkMessage(String expectedMessage) {
        assertEquals(expectedMessage, message.getText());
    }

    public boolean hasBackToHomeLink() {
        try {
            return backToHomeLink.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean hasLogOutLink() {
        try {
            return logOutLink.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean hasShowStackTraceLink() {
        try {
            return showStackTraceLink.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean hasShowContextDumpLink() {
        try {
            return showContextDumpLink.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public UserHomePage goBackToHome() {
        backToHomeLink.click();
        return asPage(UserHomePage.class);
    }

    public LoginPage goLogOut() {
        logOutLink.click();
        return asPage(LoginPage.class);
    }
}

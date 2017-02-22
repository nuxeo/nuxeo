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
 *     Kevin Leturc
 */
package org.nuxeo.functionaltests.contentView;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 9.1
 */
public class PageNavigationControls extends WebFragmentImpl {

    @Required
    @FindBy(xpath = "//input[@alt='Rewind']")
    WebElement firstButton;

    @Required
    @FindBy(xpath = "//input[@alt='Previous']")
    WebElement previousButton;

    @Required
    @FindBy(className = "currentPageStatus")
    WebElement status;

    @Required
    @FindBy(xpath = "//input[@alt='Next']")
    WebElement nextButton;

    @Required
    @FindBy(xpath = "//input[@alt='Fast Forward']")
    WebElement lastButton;

    public PageNavigationControls(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public DocumentBasePage first() {
        return first(DocumentBasePage.class);
    }

    public <T extends DocumentBasePage> T first(Class<T> pageClassToProxy) {
        return clickWithAjax(firstButton, pageClassToProxy);
    }

    public DocumentBasePage previous() {
        return previous(DocumentBasePage.class);
    }

    public <T extends DocumentBasePage> T previous(Class<T> pageClassToProxy) {
        return clickWithAjax(previousButton, pageClassToProxy);
    }

    public int getCurrentPage() {
        return Integer.parseInt(status.getText().split("/")[0]);
    }

    public int getLastPage() {
        return Integer.parseInt(status.getText().split("/")[1]);
    }

    public DocumentBasePage next() {
        return next(DocumentBasePage.class);
    }

    public <T extends DocumentBasePage> T next(Class<T> pageClassToProxy) {
        return clickWithAjax(nextButton, pageClassToProxy);
    }

    public DocumentBasePage last() {
        return last(DocumentBasePage.class);
    }

    public <T extends DocumentBasePage> T last(Class<T> pageClassToProxy) {
        return clickWithAjax(lastButton, pageClassToProxy);
    }

    protected <T extends DocumentBasePage> T clickWithAjax(WebElement button, Class<T> pageClassToProxy) {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(button);
        arm.end();
        return AbstractTest.asPage(pageClassToProxy);
    }

}

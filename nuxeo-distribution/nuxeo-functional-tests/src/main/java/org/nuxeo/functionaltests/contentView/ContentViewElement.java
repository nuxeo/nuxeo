/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva
 */
package org.nuxeo.functionaltests.contentView;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents a content view element.
 *
 * @since 7.1
 */
public class ContentViewElement extends WebFragmentImpl {

    public static enum ResultLayout {
        THUMBNAIL("Thumbnail view"), LISTING("List view");

        private final String title;

        ResultLayout(String title) {
            this.title = title;
        }
    }

    @FindBy(className = "contentViewUpperActions")
    @Required
    private WebElement upperActions;

    public ContentViewElement(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public WebElement getActionByTitle(String title) {
        return upperActions.findElement(By.xpath("//img[@alt=\"" + title + "\"]"));
    }

    public void switchToResultLayout(ResultLayout layout) {
        AjaxRequestManager a = new AjaxRequestManager(driver);
        a.watchAjaxRequests();
        getActionByTitle(layout.title).click();
        a.waitForAjaxRequests();
    }
}

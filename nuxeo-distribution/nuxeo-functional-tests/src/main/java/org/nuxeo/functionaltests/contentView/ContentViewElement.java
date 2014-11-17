/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        THUMBNAIL("Thumbnail view"),
        LISTING("List view");

        private final String title;

        ResultLayout(String title) {
            this.title = title;
        }
    }

    @FindBy(className = "contentViewUpperActions")
    @Required
    private WebElement upperActions;

    public ContentViewElement(WebDriver driver,
        WebElement element) {
        super(driver, element);
    }

    public WebElement getActionByTitle(String title) {
        return upperActions.findElement(
            By.xpath("//img[@title=\"" + title + "\"]"));
    }

    public void switchToResultLayout(ResultLayout layout) {
        AjaxRequestManager a = new AjaxRequestManager(driver);
        a.watchAjaxRequests();
        getActionByTitle(layout.title).click();
        a.waitForAjaxRequests();
    }
}

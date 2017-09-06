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

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.fragment.EditResultColumnsForm;
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
    protected WebElement upperActions;

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

    /**
     * @since 9.3
     */
    public void refresh() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        String refreshId = "nxw_contentViewActions_refreshContentView_form:nxw_contentViewActions_refreshContentView";
        Locator.findElementWaitUntilEnabledAndClick(By.id(refreshId));
        arm.end();
    }

    /**
     * @since 9.3
     */
    public EditResultColumnsForm openEditColumnsFancybox() {
        return openEditColumnsFancybox(false);
    }

    /**
     * @since 9.3
     */
    public EditResultColumnsForm openEditRowsFancybox() {
        return openEditColumnsFancybox(true);
    }

    protected EditResultColumnsForm openEditColumnsFancybox(boolean useRows) {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        String id;
        if (AbstractTest.JSF_OPTIMS_ENABLED) {
            if (useRows) {
                id = "nxw_contentViewActions_contentViewEditRows_form:nxw_contentViewActions_contentViewEditRows_link";
            } else {
                id = "nxw_contentViewActions_contentViewEditColumns_form:nxw_contentViewActions_contentViewEditColumns_link";
            }
        } else {
            if (useRows) {
                id = "nxw_contentViewActions_contentViewEditRows_form:nxw_contentViewActions_contentViewEditRows_subview:nxw_contentViewActions_contentViewEditRows_link";
            } else {
                id = "nxw_contentViewActions_contentViewEditColumns_form:nxw_contentViewActions_contentViewEditColumns_subview:nxw_contentViewActions_contentViewEditColumns_link";
            }
        }
        Locator.findElementWaitUntilEnabledAndClick(By.xpath("//a[contains(@id, '" + id + "')]"));
        arm.end();
        Locator.waitUntilElementPresent(By.id("fancybox-content"));
        return AbstractTest.getWebFragment(By.id("fancybox-content"), EditResultColumnsForm.class);
    }

}
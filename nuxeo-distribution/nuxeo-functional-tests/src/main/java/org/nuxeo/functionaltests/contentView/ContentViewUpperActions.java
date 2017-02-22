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
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.AbstractContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @since 9.1
 */
public class ContentViewUpperActions extends WebFragmentImpl {

    @FindBy(id = "nxw_contentViewActions_selectContentViewPageSize:contentViewPageSizeSelector")
    WebElement selectContentPageSize;

    @FindBy(id = "nxw_contentViewActions_refreshContentView_form:nxw_contentViewActions_refreshContentView")
    WebElement refreshContentLink;

    public ContentViewUpperActions(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public ContentTabSubPage selectPageSize(int size) {
        return selectPageSize(size, ContentTabSubPage.class);
    }

    public <T extends AbstractContentTabSubPage> T selectPageSize(int size, Class<T> pageClassToProxy) {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Select select = new Select(selectContentPageSize);
        select.selectByVisibleText(String.valueOf(size));
        arm.end();
        return AbstractTest.asPage(pageClassToProxy);
    }

    public DocumentBasePage refreshContent() {
        return refreshContent(DocumentBasePage.class);
    }

    public <T extends DocumentBasePage> T refreshContent(Class<T> pageClassToProxy) {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        refreshContentLink.click();
        arm.end();
        return AbstractTest.asPage(pageClassToProxy);
    }

    public void clickOnActionByTitle(String title) {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        getActionByTitle(title).click();
        arm.end();
    }

    public WebElement getActionByTitle(String title) {
        return getElement().findElement(By.xpath("//img[@alt=\"" + title + "\"]"));
    }

}

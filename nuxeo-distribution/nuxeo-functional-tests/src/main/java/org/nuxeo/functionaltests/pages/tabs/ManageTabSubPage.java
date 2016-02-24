/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class ManageTabSubPage extends DocumentBasePage {

    @FindBy(xpath = "//a[contains(@id,'nxw_TAB_RIGHTS')]/span")
    WebElement accessRightsLink;

    @FindBy(xpath = "//a[contains(@id,'nxw_TAB_TRASH_CONTENT')]/span")
    WebElement trashLink;

    @FindBy(xpath = "//a[contains(@id,'nxw_TAB_LOCAL_CONFIGURATION')]/span")
    WebElement localConfigLink;

    public ManageTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * @deprecated since 7.10. Use {@link PermissionsSubPage} instead.
     * @return
     */
    @Deprecated
    public AccessRightsSubPage getAccessRightsSubTab() {
        clickOnDocumentTabLink(accessRightsLink);
        return asPage(AccessRightsSubPage.class);
    }

    public TrashSubPage getTrashSubTab() {
        clickOnDocumentTabLink(trashLink);
        return asPage(TrashSubPage.class);
    }

    /**
     * @since 8.2
     */
    public LocalConfigSubPage getLocalConfigSubTabe() {
        clickOnDocumentTabLink(localConfigLink);
        return asPage(LocalConfigSubPage.class);
    }

}

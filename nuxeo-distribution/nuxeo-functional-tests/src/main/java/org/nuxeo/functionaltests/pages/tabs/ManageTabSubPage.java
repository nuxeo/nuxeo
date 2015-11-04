/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

}

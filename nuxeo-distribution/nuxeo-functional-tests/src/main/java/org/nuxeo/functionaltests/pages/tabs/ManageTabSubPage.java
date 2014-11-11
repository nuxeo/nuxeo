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
import org.openqa.selenium.support.FindBys;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class ManageTabSubPage extends DocumentBasePage {

    @FindBys({ @FindBy(id = "nxw_documentSubTabs_panel"),
            @FindBy(linkText = "Access rights") })
    WebElement accessRightsLink;

    @FindBy(linkText = "Trash")
    WebElement trashLink;

    public ManageTabSubPage(WebDriver driver) {
        super(driver);
    }

    public AccessRightsSubPage getAccessRightsSubTab() {
        clickOnLinkIfNotSelected(accessRightsLink);
        return asPage(AccessRightsSubPage.class);
    }

    public TrashSubPage getTrashSubTab() {
        clickOnLinkIfNotSelected(trashLink);
        return asPage(TrashSubPage.class);
    }

}

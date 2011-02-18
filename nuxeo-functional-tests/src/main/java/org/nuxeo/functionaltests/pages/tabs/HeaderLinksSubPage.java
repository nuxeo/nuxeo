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
 *     Benoit Delbosc
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.UserAndGroupsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HeaderLinksSubPage extends AbstractPage {

    @FindBy(linkText = "Users & groups")
    WebElement userAndGroupsLink;

    public HeaderLinksSubPage(WebDriver driver) {
        super(driver);
    }

    public UserAndGroupsPage goToUserManagementPage() {
        userAndGroupsLink.click();
        return asPage(UserAndGroupsPage.class);
    }

}

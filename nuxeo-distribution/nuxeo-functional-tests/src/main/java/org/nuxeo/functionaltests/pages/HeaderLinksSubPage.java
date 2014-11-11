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
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.pages.usermanagement.compat.UsersGroupsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HeaderLinksSubPage extends AbstractPage {

    @FindBy(linkText = "Users & Groups")
    @Deprecated
    WebElement userAndGroupsLink;

    @FindBy(xpath = "//div[@class=\"userMenuActions\"]")
    public WebElement userActions;

    @FindBy(xpath = "//div[@class=\"userMenuActions\"]/ul/li/ul")
    public WebElement userActionLinks;

    @FindBy(xpath = "//div[@class=\"userMenuActions\"]/ul/li/ul/li/a[text()=\"Log out\"]")
    WebElement logoutLink;

    public HeaderLinksSubPage(WebDriver driver) {
        super(driver);
    }

    public UsersGroupsPage goToUserManagementPage() {
        userAndGroupsLink.click();
        return asPage(UsersGroupsPage.class);
    }

    public String getText() {
        return userActions.getText();
    }

    public NavigationSubPage getNavigationSubPage() {
        return asPage(NavigationSubPage.class);
    }

}

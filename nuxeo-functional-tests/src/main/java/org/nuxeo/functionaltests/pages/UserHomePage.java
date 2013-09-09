/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.fragment.GadgetsContainerFragment;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.7
 */
public class UserHomePage extends AbstractPage {

    @Required
    @FindBy(linkText = "Dashboard")
    public WebElement dashboardLink;

    protected GadgetsContainerFragment gadgetsFragment;

    public UserHomePage(WebDriver driver) {
        super(driver);
        gadgetsFragment = getWebFragment(
                By.id(GadgetsContainerFragment.GADGETS_CONTAINER_ID),
                GadgetsContainerFragment.class);
    }

    public boolean isTaskGadgetLoaded() {
        return gadgetsFragment.isGadgetLoaded("My Tasks");
    }

    public SummaryTabSubPage redirectToTask(String taskTitle) {
        WebDriver driver = gadgetsFragment.switchToFrame("My Tasks");
        driver.findElement(By.linkText(taskTitle)).click();
        return new SummaryTabSubPage(driver);
    }

    public boolean isTaskGadgetEmpty() {
        return gadgetsFragment.isTaskGadgetEmpty("My Tasks");
    }

}

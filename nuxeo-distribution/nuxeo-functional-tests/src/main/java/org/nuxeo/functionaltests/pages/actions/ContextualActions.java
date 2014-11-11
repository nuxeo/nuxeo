/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href=mailto:vpasquier@nuxeo.com>Vladimir Pasquier</a>
 */
package org.nuxeo.functionaltests.pages.actions;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The document contextual actions
 */
public class ContextualActions extends AbstractPage {

    @FindBy(xpath = "//img[@alt=\"Lock\"]")
    public WebElement lockButton;

    @FindBy(xpath = "//img[@alt=\"Follow this document\"]")
    public WebElement followButton;

    @FindBy(xpath = "//img[@alt=\"Add to worklist\"]")
    public WebElement addToWorklistButton;

    @FindBy(id = "nxw_permalinkAction_form:nxw_documentActionsUpperButtons_permalinkAction_subview:nxw_documentActionsUpperButtons_permalinkAction_link")
    public WebElement permaButton;

    public String permaBoxFocusName = "permalinkFocus";

    @FindBy(id = "fancybox-close")
    public WebElement closePermaBoxButton;

    @FindBy(xpath = "//div[@id=\"nxw_documentActionsUpperButtons_panel\"]/div/ul/li")
    public WebElement moreButton;

    @FindBy(xpath = "//img[@alt=\"Export options\"]")
    public WebElement exportButton;

    @FindBy(xpath = "//img[@alt=\"Download\"]")
    public WebElement downloadButton;

    @FindBy(xpath = "//img[@alt=\"Add to Favorites\"]")
    public WebElement favoritesButton;

    public ContextualActions(WebDriver driver) {
        super(driver);
    }

    public void clickOnButton(WebElement button) {
        button.click();
    }
}

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

    @FindBy(xpath = "//img[@title=\"Lock\"]")
    public WebElement lockButton;

    @FindBy(xpath = "//img[@title=\"Follow this document\"]")
    public WebElement followButton;

    @FindBy(xpath = "//img[@title=\"Add to worklist\"]")
    public WebElement addToWorklistButton;

    @FindBy(xpath = "//form[@id='nxw_documentActionsUpperButtons_1_permalinkAction_form']//img")
    public WebElement permaButton;

    public String permaBoxFocusName = "permalinkFocus";

    @FindBy(id = "fancybox-close")
    public WebElement closePermaBoxButton;

    @FindBy(className = "dropDownMenu")
    public WebElement moreButton;

    @FindBy(xpath = "//img[@title=\"Export options\"]")
    public WebElement exportButton;

    @FindBy(xpath = "//img[@title=\"Download\"]")
    public WebElement downloadButton;

    public ContextualActions(WebDriver driver) {
        super(driver);
    }

    public void clickOnButton(WebElement button) {
        button.click();
    }
}

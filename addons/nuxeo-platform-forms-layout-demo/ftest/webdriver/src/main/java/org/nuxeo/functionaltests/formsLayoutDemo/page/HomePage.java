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
 *     Guillaume Renard
 *
 */

package org.nuxeo.functionaltests.formsLayoutDemo.page;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.formsLayoutDemo.page.standardWidgets.ListStandardWidgetPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 7.1
 */
public class HomePage {

    @Required
    @FindBy(linkText = "Standard Widgets")
    public WebElement standardWidgetsPanelHeaderLink;

    @Required
    @FindBy(linkText = "Listing Widgets")
    public WebElement listingWidgetsPanelHeaderLink;

    @Required
    @FindBy(linkText = "Aggregate Widgets")
    public WebElement aggregateWidgetsPanelHeaderLink;

    @Required
    @FindBy(linkText = "Action Types")
    public WebElement actionTypesPanelHeaderLink;

    @Required
    @FindBy(linkText = "Advanced Widgets")
    public WebElement advancedWidgetsPanelHeaderLink;

    public ListStandardWidgetPage goToListStandardWidget() {
        return AbstractTest.asPage(ListStandardWidgetPage.class);
    }

}

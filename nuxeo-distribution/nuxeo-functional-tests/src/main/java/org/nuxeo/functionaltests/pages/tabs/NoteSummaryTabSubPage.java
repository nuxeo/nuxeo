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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The Nuxeo summary tab of a note document.
 *
 * @since 5.9.4
 */
public class NoteSummaryTabSubPage extends AbstractPage {

    @Required
    @FindBy(xpath = "//div[@class=\"content_block\"]//td[@class=\"fieldColumn\"]")
    WebElement mainContentViewField;

    @FindBy(xpath = "//div[@class=\"textBlock note_content_block\"]")
    WebElement textBlockViewField;

    /**
     * @param driver
     */
    public NoteSummaryTabSubPage(WebDriver driver) {
        super(driver);
    }

    public String getMainContentFileText() {
        return mainContentViewField.getText();
    }

    public String getTextBlockContentText() {
        return textBlockViewField.getText();
    }

    public WebElement getTextBlockViewField() {
        return textBlockViewField;
    }
}

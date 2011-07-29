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

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class EditTabSubPage extends AbstractPage {

    @Required
    @FindBy(id="document_edit:nxl_heading:nxw_title")
    WebElement titleInputText;

    @Required
    @FindBy(id="document_edit:nxl_heading:nxw_description")
    WebElement descriptionInputText;

    @Required
    @FindBy(xpath="//input[@value=\"Save\"]")
    WebElement save;

    public EditTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * By default
     *
     * @param title
     * @param description
     * @return
     */
    public DocumentBasePage edit(String title, String description) {

        if (title != null) {
            titleInputText.clear();
            titleInputText.sendKeys(title);
        }
        if (description != null) {
            descriptionInputText.clear();
            descriptionInputText.sendKeys(description);
        }

        save.click();

        return asPage(DocumentBasePage.class);
    }

}

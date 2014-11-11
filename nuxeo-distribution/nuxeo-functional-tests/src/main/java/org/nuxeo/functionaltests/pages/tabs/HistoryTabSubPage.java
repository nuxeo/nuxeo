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
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Representation of a History tab page.
 */
public class HistoryTabSubPage extends DocumentBasePage {

    @Required
    @FindBy(linkText = "Archived versions")
    WebElement archivedVersionsLink;

    public HistoryTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Gets the archived versions sub tab.
     *
     * @return the archived versions sub tab
     */
    public ArchivedVersionsSubPage getArchivedVersionsSubTab() {
        clickOnLinkIfNotSelected(archivedVersionsLink);
        return asPage(ArchivedVersionsSubPage.class);
    }
}

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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.pages.forms;

import org.nuxeo.functionaltests.pages.tabs.CollectionContentTabSubPage;
import org.openqa.selenium.WebDriver;

/**
 * @since 5.9.3
 */
public class CollectionCreationFormPage extends DublinCoreCreationDocumentFormPage {

    /**
     * @param driver
     */
    public CollectionCreationFormPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public CollectionContentTabSubPage createDocument(String title, String description) {
        fillDublinCoreFieldsAndCreate(title, description);
        return asPage(CollectionContentTabSubPage.class);
    }

}

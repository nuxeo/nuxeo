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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.forms;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a date widget element.
 *
 * @since 5.7
 */
public class DateWidgetElement extends WidgetElement {

    public DateWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    @Override
    public WebElement getInputElement() {
        return getElement(getId() + "InputDate");
    }

}

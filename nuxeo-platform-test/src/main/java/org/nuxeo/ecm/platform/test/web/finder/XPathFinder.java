/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.platform.test.web.finder;

import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class XPathFinder implements Finder<WebElement> {

    private final String xpath;

    private final WebDriver webDriver;

    public XPathFinder(String xpath, WebDriver webDriver) {
        this.xpath = xpath;
        this.webDriver = webDriver;
    }

    public WebElement find() throws NoSuchElementException {
        return webDriver.findElement(By.xpath(xpath));
    }

    @Override
    public String toString() {
        return xpath.toString();
    }

}

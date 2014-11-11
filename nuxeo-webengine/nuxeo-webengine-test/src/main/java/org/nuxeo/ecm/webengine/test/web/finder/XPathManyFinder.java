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
package org.nuxeo.ecm.webengine.test.web.finder;

import java.util.List;
import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class XPathManyFinder implements Finder<List<WebElement>> {

    private final WebDriver driver;

    private final String xpath;

    public XPathManyFinder(String xpath, WebDriver driver) {
        this.xpath = xpath;
        this.driver = driver;
    }

    public List<WebElement> find() throws NoSuchElementException {
        List<WebElement> elements = driver.findElements(By.xpath(xpath));
        if (elements.size() == 0)
            throw new org.openqa.selenium.NoSuchElementException(
                    "No elements for xpath '" + xpath + "'");
        return elements;
    }

    @Override
    public String toString() {
        return xpath.toString();
    }

}

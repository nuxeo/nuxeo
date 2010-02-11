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

public class LinkFinder implements Finder<WebElement> {

    private final String linkPart;

    private final WebDriver driver;

    public LinkFinder(String linkPart, WebDriver driver) {
        this.linkPart = linkPart;
        this.driver = driver;
    }

    public WebElement find() throws NoSuchElementException {
        return driver.findElement(By.partialLinkText(this.linkPart));
    }

    @Override
    public String toString() {
        return linkPart;
    }

}

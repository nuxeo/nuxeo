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
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.AtomPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.RSSPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 5.9.3
 */
public class ITRSSAtomExportTest extends AbstractTest {

    @Test
    public void testRSSPage() throws UserNotConnectedException {
        login();

        get(NUXEO_URL + "/nxpath/default/default-domain@rss?contentViewName=document_content", RSSPage.class);

        WebElement rssTitle = driver.findElement(By.id("feedTitleText"));

        assertEquals("RSS feed for document 'Default domain' and content view 'Document content'", rssTitle.getText());

        get(NUXEO_URL + "/nxpath/default/default-domain@atom?contentViewName=document_content", AtomPage.class);

        WebElement atomTitle = driver.findElement(By.id("feedTitleText"));

        assertEquals("ATOM feed for document 'Default domain' and content view 'Document content'", atomTitle.getText());

    }

}

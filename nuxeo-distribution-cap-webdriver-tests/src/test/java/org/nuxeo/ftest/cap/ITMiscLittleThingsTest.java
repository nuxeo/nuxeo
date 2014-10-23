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
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * ITTest class to test very simple things.
 *
 * @since 5.9.6
 */
public class ITMiscLittleThingsTest extends AbstractTest {

    private static final String EXPECTED_HREF = "http://localhost:8080/nuxeo/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A";

    private static final String EXPECTED_ONCLICK = "if(!(event.ctrlKey||event.shiftKey||event.metaKey||event.button==1)){this.href='http:\\/\\/localhost:8080\\/nuxeo\\/nxpath\\/default\\/default-domain\\/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN'}";

    @Before
    public void setup() throws UserNotConnectedException {
        login();
    }

    @After
    public void tearDown() {
        logout();
    }

    @Test
    public void testDoubleClickShield() throws UserNotConnectedException {
        // Check that we have at least a form protected against double click
        List<WebElement> forms = driver.findElements(By.xpath("//form[contains(@class,'doubleClickShielded')]"));
        assertTrue(forms.size() > 0);
    }

    @Test
    public void testRestDocumentLinkRenderer() throws UserNotConnectedException {
        // Check that rest document link will open new tab in a new conversation
        WebElement workspaces = driver.findElement(By.linkText("Workspaces"));
        String href = workspaces.getAttribute("href");
        String onclick = workspaces.getAttribute("onclick");
        assertEquals(EXPECTED_HREF, href);
        assertEquals(EXPECTED_ONCLICK, onclick);
    }

}

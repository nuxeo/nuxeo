/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
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
    public void testRSSPage() throws UserNotConnectedException, IOException {
        login();

        getWithoutErrorHandler(NUXEO_URL + "/nxpath/default/default-domain@rss?contentViewName=document_content",
                RSSPage.class);

        WebElement rssTitle = driver.findElement(By.id("feedTitleText"));

        assertEquals("RSS Feed for Document 'Domain' and Content View 'Document content'", rssTitle.getText());

        getWithoutErrorHandler(NUXEO_URL + "/nxpath/default/default-domain@atom?contentViewName=document_content",
                AtomPage.class);

        WebElement atomTitle = driver.findElement(By.id("feedTitleText"));

        assertEquals("ATOM Feed for Document 'Domain' and Content View 'Document content'", atomTitle.getText());

    }

}

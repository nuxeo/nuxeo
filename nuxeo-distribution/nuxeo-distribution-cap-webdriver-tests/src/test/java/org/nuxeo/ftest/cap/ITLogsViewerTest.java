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
 *     Thomas Roger
 */

package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.monitoring.LogsPage;
import org.openqa.selenium.WebElement;

/**
 * Test Logs Viewer tab
 *
 * @since 6.0
 */
public class ITLogsViewerTest extends AbstractTest {

    @Test
    public void testLogsViewerTab() throws DocumentBasePage.UserNotConnectedException {
        LogsPage logsTab = login().getAdminCenter().getMonitoringPage().getLogsPage();
        List<String> serverLogFileNames = logsTab.getServerLogFileNames();
        assertEquals(7, serverLogFileNames.size());
        assertTrue(serverLogFileNames.contains("server.log"));
        assertTrue(serverLogFileNames.contains("classloader.log"));
        assertTrue(serverLogFileNames.contains("nuxeo-error.log"));
        assertTrue(serverLogFileNames.contains("tomcat.log"));
        assertTrue(serverLogFileNames.contains("stderr.log"));
        assertTrue(serverLogFileNames.contains("nuxeoctl.log"));
        assertTrue(serverLogFileNames.contains("console.log"));

        logsTab = logsTab.selectServerLogFileTab("server.log");
        WebElement downloadButton = logsTab.getDownloadButton("server.log");
        assertNotNull(downloadButton);
    }
}

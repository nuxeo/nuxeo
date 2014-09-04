/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.monitoring.LogsPage;
import org.openqa.selenium.WebElement;

/**
 * Test Logs Viewer tab
 *
 * @since 5.9.6
 */
public class ITLogsViewerTest extends AbstractTest {

    @Test
    public void testLogsViewerTab()
            throws DocumentBasePage.UserNotConnectedException {
        LogsPage logsTab = login().getAdminCenter().getMonitoringPage().getLogsPage();
        List<String> serverLogFileNames = logsTab.getServerLogFileNames();
        assertEquals(5, serverLogFileNames.size());
        assertTrue(serverLogFileNames.contains("server.log"));
        assertTrue(serverLogFileNames.contains("classloader.log"));
        assertTrue(serverLogFileNames.contains("nuxeo-error.log"));
        assertTrue(serverLogFileNames.contains("tomcat.log"));
        assertTrue(serverLogFileNames.contains("stderr.log"));

        logsTab = logsTab.selectServerLogFileTab("server.log");
        WebElement downloadButton = logsTab.getDownloadButton("server.log");
        assertNotNull(downloadButton);
    }
}

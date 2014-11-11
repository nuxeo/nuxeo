/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Guillaume Renard
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Test Drag and Drop Import feature.
 *
 * @since 5.9.1
 */
public class ITDnDImportTest extends AbstractTest {

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    private final static String CVDZ_ID = "CVDZ";

    private final static String DROP_ZONE_TARGET_CSS_CLASS = "dropzoneTarget";

    private final static String DROP_ZONE_CSS_CLASS = "dropzone";

    @FindBy(xpath = "//div[@id='CVDZ']")
    public WebElement dropZone;

    private JavascriptExecutor js;

    private void dragoverMockFileFF() {
        js.executeScript(String.format("jQuery('#%s').dropzone();", CVDZ_ID));
        js.executeScript(String.format(
                "var dragoverEvent = window.document.createEvent('DragEvents');"
                        + "dragoverEvent.initDragEvent('dragover', true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null, null);"
                        + "var target = document.getElementById('%s');"
                        + "target.dispatchEvent(dragoverEvent);", CVDZ_ID));
        js.executeScript(String.format(
                "var dragenterEvent = window.document.createEvent('DragEvents');"
                        + "dragenterEvent.initDragEvent('dragenter', true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null, null);"
                        + "var target = document.getElementById('%s');"
                        + "target.dispatchEvent(dragenterEvent);", CVDZ_ID));

    }

    private void dropMockFileFF() {
        js.executeScript(String.format(
                "var dataTransfer = "
                        + "{\"items\":{\"0\":{\"type\":\"\",\"kind\":\"file\"},\"length\":1},"
                        + "\"files\":{\"0\":{\"webkitRelativePath\":\"\",\"lastModifiedDate\":\"\'2013-10-29T11:05:01Z\'\",\"name\":\"messages_en.properties\",\"type\":\"\",\"size\":88073},\"length\":1},\"types\":[\"Files\"],\"effectAllowed\":\"all\",\"dropEffect\":\"none\"};"
                        + "var dropEvent = window.document.createEvent('DragEvents');"
                        + "dropEvent.initDragEvent('drop', true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null, dataTransfer);"
                        + "var target = document.getElementById('%s');"
                        + "target.dispatchEvent(dropEvent);", CVDZ_ID));
    }

    /**
     * Test that the drop zone is reactive.
     *
     * @throws UserNotConnectedException
     *
     * @since 5.9.1
     */
    @Test
    public void testDropZone() throws UserNotConnectedException {
        this.js = driver;

        DocumentBasePage documentBasePage = login();

        documentBasePage = documentBasePage.getNavigationSubPage().goToDocument(
                "Workspaces");

        createWorkspace(documentBasePage, WORKSPACE_TITLE, null);

        WebElement dropzone = driver.findElement(By.id(CVDZ_ID));

        // Check that
        String cssClass = dropzone.getAttribute("class");
        assertNotNull(cssClass);
        assertTrue(cssClass.contains(DROP_ZONE_CSS_CLASS));
        assertFalse(cssClass.contains(DROP_ZONE_TARGET_CSS_CLASS));

        dragoverMockFileFF();

        // Check that the dropzone has detected the the dragover
        cssClass = dropzone.getAttribute("class");
        assertNotNull(cssClass);
        assertTrue(cssClass.contains(DROP_ZONE_TARGET_CSS_CLASS));

        dropMockFileFF();

        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);

        logout();
    }

}

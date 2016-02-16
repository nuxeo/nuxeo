/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.tabs.RelationTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.NXDOC_URL_FORMAT;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Document Relations test.
 *
 * @since 5.9.1
 */
public class ITDocumentRelationTest extends AbstractTest {

    private final static String WORKSPACE_TITLE = ITDocumentRelationTest.class.getSimpleName() + "_WorkspaceTitle_"
            + new Date().getTime();

    private final String FILE_NAME1 = "File1";

    private final String FILE_NAME2 = "File2";

    private static String wsId;

    @Before
    public void setUp() throws UserNotConnectedException, IOException {
        wsId = RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE_TITLE, null);
        RestHelper.createDocument(wsId, FILE_TYPE, FILE_NAME1, null);
        RestHelper.createDocument(wsId, FILE_TYPE, FILE_NAME2, null);
    }

    @After
    public void tearDown() throws UserNotConnectedException {
        RestHelper.cleanup();
        wsId = null;
    }

    /**
     * Create a relation between 2 documents and delete it.
     *
     * @throws UserNotConnectedException
     * @since 5.9.1
     */
    @Test
    public void testSimpleRelationBetweenTwoDocuments() throws UserNotConnectedException {
        login();
        open(String.format(NXDOC_URL_FORMAT, wsId));

        DocumentBasePage documentBasePage = asPage(DocumentBasePage.class).getContentTab().goToDocument(FILE_NAME1);

        RelationTabSubPage relationTabSubPage = documentBasePage.getRelationTab();

        relationTabSubPage = relationTabSubPage.initRelationSetUp();

        relationTabSubPage = relationTabSubPage.setRelationWithDocument(FILE_NAME2,
                "http://purl.org/dc/terms/ConformsTo");

        List<WebElement> existingRelations = relationTabSubPage.getExistingRelations();
        assertNotNull(existingRelations);

        assertEquals(1, existingRelations.size());

        WebElement newRelation = existingRelations.get(0);

        assertEquals("Conforms to", newRelation.findElement(By.xpath("td[1]")).getText());

        assertNotNull(newRelation.findElement(By.linkText(FILE_NAME2)));

        relationTabSubPage = relationTabSubPage.deleteRelation(0);

        assertEquals(0, relationTabSubPage.getExistingRelations().size());

        logout();
    }

}

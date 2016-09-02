/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.ftest.cap;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Constants;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.fragment.AddAllToCollectionForm;
import org.nuxeo.functionaltests.fragment.AddToCollectionForm;
import org.nuxeo.functionaltests.pages.CollectionsPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.HomePage;
import org.nuxeo.functionaltests.pages.NavigationSubPage;
import org.nuxeo.functionaltests.pages.tabs.CollectionContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.ManageTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test Collection feature.
 *
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ FakeSmtpMailServerFeature.class })
public class ITCollectionsTest extends AbstractTest {

    public final static String TEST_FILE_NAME = "test1";

    public static final String TEST_FILE_PATH = TEST_WORKSPACE_PATH + TEST_FILE_NAME;

    public static final String TEST_FILE_URL = String.format(Constants.NXPATH_URL_FORMAT, TEST_FILE_PATH);

    public final static String TEST_FILE_NAME2 = "test2";

    public static final String TEST_FILE_PATH2 = TEST_WORKSPACE_PATH + TEST_FILE_NAME2;

    public static final String TEST_FILE_URL2 = String.format(Constants.NXPATH_URL_FORMAT, TEST_FILE_PATH2);

    public final static String COLLECTION_NAME_1 = "Collection1";

    public final static String COLLECTION_DESCRIPTION_1 = "My first collection";

    public final static String COLLECTION_NAME_2 = "Collection2";

    private static final String COLLECTION_DESCRIPTION_2 = "My second collection";

    private static final String CAN_COLLECT_RIGHT = "Can Collect";

    public static final String MY_COLLECTIONS_FR_LABEL = "Mes collections";

    public static final String MY_COLLECTIONS_EN_LABEL = "My Collections";

    public static final String MY_FAVORITES_FR_LABEL = "Mes favoris";

    public static final String MY_FAVORITES_EN_LABEL = "My Favorites";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_NAME, "Test File description");
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_NAME2, "Test File description");
    }

    @After
    public void tearDown() throws UserNotConnectedException {
        RestHelper.cleanup();

        DocumentBasePage documentBasePage = login();
        ContentTabSubPage contentTabSubPage = documentBasePage.switchToPersonalWorkspace().getContentTab();
        contentTabSubPage = contentTabSubPage.removeAllDocuments();
        ManageTabSubPage manageTabSubPage = contentTabSubPage.getManageTab();
        manageTabSubPage.getTrashSubTab().purgeAllDocuments();
        logout();
    }

    @Test
    public void testAddDocumentToCollectionAndRemove() throws UserNotConnectedException, IOException {

        DocumentBasePage documentBasePage = login();
        // Check we can not add Domain to a collection
        assertFalse(documentBasePage.isAddToCollectionUpperActionAvailable());

        // Open test file page
        open(TEST_FILE_URL);
        FileDocumentBasePage fileDocumentBasePage = asPage(FileDocumentBasePage.class);

        // Check that collection widget summary is not displayed
        SummaryTabSubPage summaryTabSubPage = fileDocumentBasePage.getSummaryTab();
        assertFalse(summaryTabSubPage.isCollectionsFormDisplayed());

        // Simple add to collection
        AddToCollectionForm addToCollectionForm = fileDocumentBasePage.getAddToCollectionPopup();

        addToCollectionForm.setCollection(COLLECTION_NAME_1);

        addToCollectionForm.setNewDescription(COLLECTION_DESCRIPTION_1);

        fileDocumentBasePage = addToCollectionForm.add(FileDocumentBasePage.class);

        // Check that collection widget summary is displayed
        summaryTabSubPage = fileDocumentBasePage.getSummaryTab();
        assertTrue(summaryTabSubPage.isCollectionsFormDisplayed());
        assertEquals(1, summaryTabSubPage.getCollectionCount());

        // Open test file 2 page
        open(TEST_FILE_URL2);

        addToCollectionForm = fileDocumentBasePage.getAddToCollectionPopup();

        addToCollectionForm.setCollection(COLLECTION_NAME_1);

        assertFalse(addToCollectionForm.isNewDescriptionVisible());

        assertTrue(addToCollectionForm.isExistingDescriptionVisible());

        assertEquals(COLLECTION_DESCRIPTION_1, addToCollectionForm.getExistingDescription());

        fileDocumentBasePage = addToCollectionForm.add(FileDocumentBasePage.class);

        // Multiple add to collection
        ContentTabSubPage workspaceContentTab = fileDocumentBasePage.getNavigationSubPage()
                                                                    .goToDocument(TEST_WORKSPACE_TITLE)
                                                                    .getContentTab();

        workspaceContentTab.selectByIndex(0, 1);

        AddAllToCollectionForm addAllToCollectionForm = workspaceContentTab.getAddAllToCollectionPopup();

        addAllToCollectionForm.setCollection(COLLECTION_NAME_2);

        assertTrue(addAllToCollectionForm.isNewDescriptionVisible());

        addAllToCollectionForm.setNewDescription(COLLECTION_DESCRIPTION_2);

        workspaceContentTab = addAllToCollectionForm.addAll(ContentTabSubPage.class);

        // Check Collections section in HOME
        CollectionsPage collectionsPage = fileDocumentBasePage.goToHomePage().goToCollections();

        List<String> collectionNames = collectionsPage.getCollectionNames();

        assertEquals(2, collectionNames.size());
        assertEquals(COLLECTION_NAME_1, collectionNames.get(0));
        assertEquals(COLLECTION_NAME_2, collectionNames.get(1));

        ContentTabSubPage contentTabSubPage = workspaceContentTab.switchToPersonalWorkspace().getContentTab();

        // Check Collection are stored in Personal Workspace
        List<WebElement> personalWorkspaceRootDocs = contentTabSubPage.getChildDocumentRows();

        assertEquals(2, personalWorkspaceRootDocs.size());
        final String myCollectionsDocName = personalWorkspaceRootDocs.get(0).findElement(By.xpath("td[3]")).getText();
        boolean isFrench = MY_COLLECTIONS_FR_LABEL.equals(myCollectionsDocName);
        boolean isEnglish = MY_COLLECTIONS_EN_LABEL.equals(myCollectionsDocName);

        assertTrue(isEnglish || isFrench);
        final String myFavoritesDocName = personalWorkspaceRootDocs.get(1).findElement(By.xpath("td[3]")).getText();
        assertEquals(isEnglish ? MY_FAVORITES_EN_LABEL : MY_FAVORITES_FR_LABEL, myFavoritesDocName);

        contentTabSubPage.switchToDocumentBase();

        CollectionContentTabSubPage collectionContentTabSubPage = contentTabSubPage.goToHomePage()
                                                                                   .goToCollections()
                                                                                   .goToCollection(COLLECTION_NAME_1);
        assertEquals(2, collectionContentTabSubPage.getChildDocumentRows().size());
        // check home tab context is ok
        HomePage hp = asPage(HomePage.class);
        assertTrue(hp.isMainTabSelected(hp.homePageLink));

        collectionContentTabSubPage = collectionContentTabSubPage.goToHomePage()
                                                                 .goToCollections()
                                                                 .goToCollection(COLLECTION_NAME_2);

        assertEquals(2, collectionContentTabSubPage.getChildDocumentRows().size());

        // Check copy/paste collection
        // navigate back to user workspace root
        contentTabSubPage = collectionContentTabSubPage.switchToPersonalWorkspace().getContentTab();

        contentTabSubPage = contentTabSubPage.goToDocument(
                isEnglish ? MY_COLLECTIONS_EN_LABEL : MY_COLLECTIONS_FR_LABEL).getContentTab();

        contentTabSubPage.copyByTitle(COLLECTION_NAME_1);

        contentTabSubPage = contentTabSubPage.paste();

        assertEquals(3, contentTabSubPage.getChildDocumentRows().size());

        contentTabSubPage = contentTabSubPage.goToDocument(1).getCollectionContentTab();

        assertEquals(2, contentTabSubPage.getChildDocumentRows().size());

        // TODO test collection remove

        logout();
    }

    @Test
    public void testRightsOnCollection() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();

        // Create 2 collections in "My Collections" container
        documentBasePage = documentBasePage.switchToPersonalWorkspace();
        DocumentBasePage workspacePage = documentBasePage.getNavigationSubPage().goToDocument("Administrator");
        // Add a file to this collection that test user can't see
        FileDocumentBasePage fileDocumentBasePage = workspacePage.createFile(TEST_FILE_NAME, "Test File description",
                false, null, null, null);
        AddToCollectionForm addToCollectionForm = fileDocumentBasePage.getAddToCollectionPopup();
        addToCollectionForm.setCollection(COLLECTION_NAME_2);
        fileDocumentBasePage = addToCollectionForm.add(FileDocumentBasePage.class);

        NavigationSubPage navigationSubPage = fileDocumentBasePage.getNavigationSubPage();
        if (navigationSubPage.canNavigateToDocument(MY_COLLECTIONS_EN_LABEL)) {
            documentBasePage = fileDocumentBasePage.getNavigationSubPage().goToDocument(MY_COLLECTIONS_EN_LABEL);
        } else {
            documentBasePage = fileDocumentBasePage.getNavigationSubPage().goToDocument(MY_COLLECTIONS_FR_LABEL);
        }

        documentBasePage = workspacePage.createCollection(COLLECTION_NAME_2, COLLECTION_DESCRIPTION_2);

        documentBasePage.getPermissionsTab().grantPermission(CAN_COLLECT_RIGHT, TEST_USERNAME);

        logout();

        // Login as test user
        documentBasePage = loginAsTestUser();
        CollectionsPage collectionsPage = documentBasePage.goToHomePage().goToCollections();
        List<String> collections = collectionsPage.getCollectionNames();
        // Check that I can see the second collection but not the first one
        assertEquals(1, collections.size());
        assertTrue(COLLECTION_NAME_2.equals(collections.get(0)));

        // Check that I can't see the file Admin added
        CollectionContentTabSubPage collectionContentTabSubPage = collectionsPage.goToCollection(COLLECTION_NAME_2);
        assertEquals(0, collectionContentTabSubPage.getChildDocumentRows().size());

        // Create a file in test user workspace and add it to the collection
        documentBasePage = collectionContentTabSubPage.switchToPersonalWorkspace();
        fileDocumentBasePage = workspacePage.createFile(TEST_FILE_NAME, "Test File description", false, null, null,
                null);
        addToCollectionForm = fileDocumentBasePage.getAddToCollectionPopup();
        addToCollectionForm.setCollection(COLLECTION_NAME_2);
        fileDocumentBasePage = addToCollectionForm.add(FileDocumentBasePage.class);

        // Check now the collection has one file
        collectionsPage = asPage(DocumentBasePage.class).goToHomePage().goToCollections();
        collectionContentTabSubPage = collectionsPage.goToCollection(COLLECTION_NAME_2);
        assertEquals(1, collectionContentTabSubPage.getChildDocumentRows().size());

        logout();
    }

}

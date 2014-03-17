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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.functionaltests.forms.AddToCollectionForm;
import org.nuxeo.functionaltests.pages.CollectionsPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.AccessRightsSubPage;
import org.nuxeo.functionaltests.pages.tabs.CollectionContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.ManageTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test Collection feature.
 *
 * @since 5.9.3
 */
public class ITCollectionsTest extends AbstractTest {

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    public final static String TEST_FILE_NAME = "test1";

    public final static String TEST_FILE_NAME2 = "test2";

    public final static String COLLECTION_NAME_1 = "Collection1";

    public final static String COLLECTION_DESSCRIPTION_1 = "My first collection";

    public final static String COLLECTION_NAME_2 = "Collection2";

    private static final String COLLECTION_DESSCRIPTION_2 = "My second collection";

    private static final String CAN_COLLECT_RIGHT = "Can Collect";

    @After
    public void tearDown() throws UserNotConnectedException {
        DocumentBasePage documentBasePage = login();
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (usersTab.isUserFound(TEST_USERNAME)) {
            usersTab = usersTab.viewUser(TEST_USERNAME).deleteUser();
            documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                    "Workspaces");
        }
        ContentTabSubPage contentTabSubPage = documentBasePage.swithToPersonalWorkspace().getContentTab();
        contentTabSubPage = contentTabSubPage.removeAllDocuments();
        ManageTabSubPage manageTabSubPage = contentTabSubPage.getManageTab();
        manageTabSubPage.getTrashSubTab().purgeAllDocuments();
        asPage(DocumentBasePage.class).swithToDocumentBase();
        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);
        logout();
    }

    @Test
    public void testAddDocumentToCollectionAndRemove()
            throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();
        // Check we can not add Domain to a collection
        assertFalse(documentBasePage.isAddToCollectionUpperActionAvailable());
        // Create test File
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);

        FileDocumentBasePage fileDocumentBasePage = createFile(workspacePage,
                TEST_FILE_NAME, "Test File description", false, null, null,
                null);

        // Simple add to collection
        AddToCollectionForm addToCollectionForm = fileDocumentBasePage.getAddToCollectionPopup();

        addToCollectionForm.setCollection(COLLECTION_NAME_1);

        addToCollectionForm.setNewDescription(COLLECTION_DESSCRIPTION_1);

        fileDocumentBasePage = addToCollectionForm.add(FileDocumentBasePage.class);

        workspacePage = fileDocumentBasePage.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE);

        fileDocumentBasePage = createFile(workspacePage, TEST_FILE_NAME2,
                "Test File description", false, null, null, null);

        addToCollectionForm = fileDocumentBasePage.getAddToCollectionPopup();

        addToCollectionForm.setCollection(COLLECTION_NAME_1);

        assertFalse(addToCollectionForm.isNewDescriptionVisible());

        assertTrue(addToCollectionForm.isExistingDescriptionVisible());

        assertEquals(COLLECTION_DESSCRIPTION_1,
                addToCollectionForm.getExistingDescription());

        fileDocumentBasePage = addToCollectionForm.add(FileDocumentBasePage.class);

        // Multiple add to collection
        ContentTabSubPage workspaceContentTab = fileDocumentBasePage.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE).getContentTab();

        workspaceContentTab.selectDocumentByIndex(0, 1);

        addToCollectionForm = workspaceContentTab.getAddAllToCollectionPopup();

        addToCollectionForm.setCollection(COLLECTION_NAME_2);

        assertTrue(addToCollectionForm.isNewDescriptionVisible());

        addToCollectionForm.setNewDescription(COLLECTION_DESSCRIPTION_2);

        workspaceContentTab = addToCollectionForm.addAll(ContentTabSubPage.class);

        // Check Collections section in HOME
        CollectionsPage collectionsPage = fileDocumentBasePage.goToHomePage().goToCollections();

        List<String> collectionNames = collectionsPage.getCollectionNames();

        assertEquals(2, collectionNames.size());
        assertEquals(COLLECTION_NAME_1, collectionNames.get(0));
        assertEquals(COLLECTION_NAME_2, collectionNames.get(1));

        ContentTabSubPage contentTabSubPage = workspaceContentTab.swithToPersonalWorkspace().getContentTab();

        // Check Collection are stored in Personal Workspace
        List<WebElement> personalWorkspaceRootDocs = contentTabSubPage.getChildDocumentRows();

        assertEquals(1, personalWorkspaceRootDocs.size());
        assertEquals(
                "Collections",
                personalWorkspaceRootDocs.get(0).findElement(By.xpath("td[3]")).getText());

        contentTabSubPage.swithToDocumentBase();

        CollectionContentTabSubPage collectionContentTabSubPage = contentTabSubPage.goToHomePage().goToCollections().gotToCollection(
                COLLECTION_NAME_1);

        assertEquals(2,
                collectionContentTabSubPage.getChildDocumentRows().size());

        collectionContentTabSubPage = collectionContentTabSubPage.goToHomePage().goToCollections().gotToCollection(
                COLLECTION_NAME_2);

        assertEquals(2,
                collectionContentTabSubPage.getChildDocumentRows().size());

        // Check copy/paste collection
        contentTabSubPage = collectionContentTabSubPage.swithToPersonalWorkspace().getContentTab();

        contentTabSubPage = contentTabSubPage.goToDocument("Collections").getContentTab();

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
        // Create test user if not exist
        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = documentBasePage.getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (!usersTab.isUserFound(TEST_USERNAME)) {
            page = usersTab.getUserCreatePage().createUser(TEST_USERNAME,
                    TEST_USERNAME, "lastname1", "company1", "email1",
                    TEST_PASSWORD, "members");
            usersTab = page.getUsersTab(true);
        } else {
            throw new IllegalStateException(String.format("user %s already exists", TEST_USERNAME));
        }
        usersTab.searchUser(TEST_USERNAME);
        assertTrue(usersTab.isUserFound(TEST_USERNAME));

        documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");

        // Create workspace and deny all to test user
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);
        AccessRightsSubPage accessRightSubTab = workspacePage.getManageTab().getAccessRightsSubTab();
        if (!accessRightSubTab.hasPermissionForUser("Manage everything",
                TEST_USERNAME)) {
            accessRightSubTab.addPermissionForUser(TEST_USERNAME, "Read", false);
        }

        // Create 2 collections in "My Collections" container
        documentBasePage = createCollections(workspacePage, "My Collections",
                null);
        documentBasePage = createCollection(workspacePage, COLLECTION_NAME_1,
                COLLECTION_DESSCRIPTION_1);
        documentBasePage = documentBasePage.getCollectionContentTab().getNavigationSubPage().goToDocument(
                "My Collections");
        documentBasePage = createCollection(workspacePage, COLLECTION_NAME_2,
                COLLECTION_DESSCRIPTION_2);

        // Give "Can Collect" to test user to the second one
        documentBasePage.getManageTab().getAccessRightsSubTab().addPermissionForUser(
                TEST_USERNAME, CAN_COLLECT_RIGHT, true);
        documentBasePage = documentBasePage.swithToPersonalWorkspace();
        workspacePage = documentBasePage.getNavigationSubPage().goToDocument(
                "Administrator");
        // Add a file to this collection that test user can't see
        FileDocumentBasePage fileDocumentBasePage = createFile(workspacePage,
                TEST_FILE_NAME, "Test File description", false, null, null,
                null);
        AddToCollectionForm addToCollectionForm = fileDocumentBasePage.getAddToCollectionPopup();
        addToCollectionForm.setCollection(COLLECTION_NAME_2);
        fileDocumentBasePage = addToCollectionForm.add(FileDocumentBasePage.class);
        logout();

        // Login as test user
        documentBasePage = loginAsTestUser();
        CollectionsPage collectionsPage = documentBasePage.goToHomePage().goToCollections();
        List<String> collections = collectionsPage.getCollectionNames();
        // Check that I can see the second collection but not the first one
        assertEquals(1, collections.size());
        assertTrue(COLLECTION_NAME_2.equals(collections.get(0)));

        // Check that I can't see the file Admin added
        CollectionContentTabSubPage collectionContentTabSubPage = collectionsPage.gotToCollection(COLLECTION_NAME_2);
        assertEquals(0,
                collectionContentTabSubPage.getChildDocumentRows().size());

        // Create a file in test user workspace and add it to the collection
        documentBasePage = collectionContentTabSubPage.swithToPersonalWorkspace();
        workspacePage = documentBasePage.getNavigationSubPage().goToDocument(
                "jdoe lastname1");
        fileDocumentBasePage = createFile(workspacePage, TEST_FILE_NAME,
                "Test File description", false, null, null, null);
        addToCollectionForm = fileDocumentBasePage.getAddToCollectionPopup();
        addToCollectionForm.setCollection(COLLECTION_NAME_2);
        fileDocumentBasePage = addToCollectionForm.add(FileDocumentBasePage.class);

        // Check now the collection has one file
        collectionsPage = documentBasePage.goToHomePage().goToCollections();
        collectionContentTabSubPage = collectionsPage.gotToCollection(COLLECTION_NAME_2);
        assertEquals(1,
                collectionContentTabSubPage.getChildDocumentRows().size());

        logout();
    }

}

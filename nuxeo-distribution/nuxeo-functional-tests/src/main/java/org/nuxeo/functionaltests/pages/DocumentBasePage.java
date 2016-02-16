/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.fragment.AddAllToCollectionForm;
import org.nuxeo.functionaltests.fragment.AddToCollectionForm;
import org.nuxeo.functionaltests.pages.actions.ContextualActions;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.nuxeo.functionaltests.pages.forms.CollectionCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.DublinCoreCreationDocumentFormPage;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.NoteCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceCreationFormPage;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.nuxeo.functionaltests.pages.tabs.CollectionContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.CommentsTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.HistoryTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.ManageTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.PermissionsSubPage;
import org.nuxeo.functionaltests.pages.tabs.RelationTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.WorkflowTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.google.common.base.Function;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.NOTE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_TITLE;

import static org.junit.Assert.assertEquals;

/**
 * The nuxeo main document base page
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class DocumentBasePage extends AbstractPage {

    /**
     * Exception occurred a user is expected to be connected but it isn't.
     */
    public class UserNotConnectedException extends Exception {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public UserNotConnectedException(String username) {
            super("The user " + username + " is expected to be connected but isn't");
        }
    }

    @FindBy(xpath = "//form[@id='breadcrumbForm']")
    public WebElement breadcrumbForm;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Content']")
    public WebElement contentTabLink;

    public ContextualActions contextualActions;

    @FindBy(className = "creator")
    public WebElement currentDocumentContributor;

    @FindBy(className = "documentDescription")
    public WebElement currentDocumentDescription;

    @FindBy(xpath = "//form[@id='document_header_layout_form']//h1")
    public WebElement currentDocumentTitle;

    @FindBy(className = "currentDocumentDescription")
    public WebElement currentFolderishDescription;

    @FindBy(linkText = "WORKSPACE")
    public WebElement documentManagementLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Edit']")
    public WebElement editTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Permissions']")
    public WebElement permissionsTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='History']")
    public WebElement historyTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Manage']")
    public WebElement manageTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Relations']")
    public WebElement relationTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Summary']")
    public WebElement summaryTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Comments']")
    public WebElement commentsTabLink;

    @Required
    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']")
    public WebElement tabsBar;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Workflow']")
    public WebElement workflowLink;

    @Required
    @FindBy(id = "nxw_userMenuActions_panel")
    public WebElement userMenuActions;

    @Required
    @FindBy(linkText = "HOME")
    public WebElement homePageLink;

    @Required
    @FindBy(linkText = "SEARCH")
    public WebElement searchPageLink;

    public DocumentBasePage(WebDriver driver) {
        super(driver);
    }

    /**
     * Check if the title of the current document page is equal to the {@code expectedTitle}.
     *
     * @param expectedTitle the expected title
     */
    public void checkDocTitle(String expectedTitle) {
        assertEquals(expectedTitle, getCurrentDocumentTitle());
    }

    /**
     * Check if the user is connected by looking for an element with the {@code username} as a class.
     *
     * @param username
     * @throws UserNotConnectedException
     */
    public void checkUserConnected(String username) throws UserNotConnectedException {
        try {
            findElementWithTimeout(By.cssSelector("span." + username));
        } catch (NoSuchElementException e) {
            throw new UserNotConnectedException(username);
        }
    }

    /**
     * @since 7.10
     */
    public void clickOnDocumentTabLink(WebElement tabLink) {
        clickOnDocumentTabLink(tabLink, useAjaxTabs());
    }

    /**
     * Clicks on given tab element, detecting begin and end of potential ajax request.
     *
     * @since 8.1
     */
    public void clickOnDocumentTabLink(WebElement tabLink, boolean useAjax) {
        clickOnTabIfNotSelected("nxw_documentTabs_panel", tabLink, useAjax);
    }

    public AdminCenterBasePage getAdminCenter() {
        findElementWithTimeout(By.linkText("ADMIN")).click();
        return asPage(AdminCenterBasePage.class);
    }

    /**
     * Click on the content tab and return the subpage of this page.
     */
    public ContentTabSubPage getContentTab() {
        return getContentTab(ContentTabSubPage.class);
    }

    public <T extends ContentTabSubPage> T getContentTab(Class<T> tabClass) {
        clickOnDocumentTabLink(contentTabLink);
        return asPage(tabClass);
    }

    public CollectionContentTabSubPage getCollectionContentTab() {
        return getContentTab(CollectionContentTabSubPage.class);
    }

    public ContextualActions getContextualActions() {
        return asPage(ContextualActions.class);
    }

    /**
     * @since 7.3
     */
    public List<WebElement> getBlobActions(int index) {
        return findElementsWithTimeout(By.xpath("(//div[@class='actionsColumn'])[" + (index + 1) + "]//a"));
    }

    public String getCurrentContributors() {
        return currentDocumentContributor.getText();
    }

    public String getCurrentDocumentDescription() {
        return currentDocumentDescription.getText();
    }

    public String getCurrentDocumentTitle() {
        return currentDocumentTitle.getText();
    }

    public String getCurrentFolderishDescription() {
        return currentFolderishDescription.getText();
    }

    public List<String> getCurrentStates() {
        List<WebElement> states = findElementsWithTimeout(By.className("sticker"));
        List<String> stateLabels = new ArrayList<String>();
        for (WebElement state : states) {
            stateLabels.add(state.getText());
        }
        return stateLabels;
    }

    /**
     * @deprecated since 7.3: use {@link #goToWorkspaces()} instead
     */
    public DocumentBasePage getDocumentManagement() {
        return goToWorkspaces();
    }

    public EditTabSubPage getEditTab() {
        clickOnDocumentTabLink(editTabLink);
        return asPage(EditTabSubPage.class);
    }

    public PermissionsSubPage getPermissionsTab() {
        // not ajaxified
        clickOnDocumentTabLink(permissionsTabLink, false);
        return asPage(PermissionsSubPage.class);
    }

    public HistoryTabSubPage getHistoryTab() {
        clickOnDocumentTabLink(historyTabLink);
        return asPage(HistoryTabSubPage.class);
    }

    public ManageTabSubPage getManageTab() {
        clickOnDocumentTabLink(manageTabLink);
        return asPage(ManageTabSubPage.class);
    }

    public NavigationSubPage getNavigationSubPage() {
        return asPage(NavigationSubPage.class);
    }

    public RelationTabSubPage getRelationTab() {
        clickOnDocumentTabLink(relationTabLink);
        return asPage(RelationTabSubPage.class);
    }

    public SummaryTabSubPage getSummaryTab() {
        clickOnDocumentTabLink(summaryTabLink);
        return asPage(SummaryTabSubPage.class);
    }

    public CommentsTabSubPage getCommentsTab() {
        clickOnDocumentTabLink(commentsTabLink);
        return asPage(CommentsTabSubPage.class);
    }

    /**
     * @since 5.7
     */
    public UserHomePage getUserHome() {
        findElementWithTimeout(By.linkText("HOME")).click();
        UserHomePage page = asPage(UserHomePage.class);
        // make sure we're back on the dashboard tab
        return page.goToDashboard();
    }

    public WorkflowTabSubPage getWorkflow() {
        clickOnDocumentTabLink(workflowLink);
        return asPage(WorkflowTabSubPage.class);
    }

    /**
     * For workspace type, the content tab is a bit different.
     */
    public WorkspacesContentTabSubPage getWorkspacesContentTab() {
        clickOnDocumentTabLink(contentTabLink);
        return asPage(WorkspacesContentTabSubPage.class);
    }

    public DocumentBasePage goToDocumentByBreadcrumb(String documentTitle) {
        breadcrumbForm.findElement(By.linkText(documentTitle)).click();
        return asPage(DocumentBasePage.class);
    }

    private static final String ADD_TO_COLLECTION_UPPER_ACTION_ID = "nxw_addToCollectionAction_form:nxw_addToCollectionAction_link";

    private static final String ADD_ALL_TO_COLLECTION_ACTION_ID = "document_content_buttons:nxw_addSelectedToCollectionAction_form:nxw_addSelectedToCollectionAction_link";

    @FindBy(id = ADD_TO_COLLECTION_UPPER_ACTION_ID)
    private WebElement addToCollectionUpperAction;

    @FindBy(id = ADD_ALL_TO_COLLECTION_ACTION_ID)
    private WebElement addAllToCollectionAction;

    /**
     * @since 5.9.3
     */
    public AddToCollectionForm getAddToCollectionPopup() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        addToCollectionUpperAction.click();
        arm.end();
        Locator.waitUntilElementPresent(By.id("fancybox-content"));
        return getWebFragment(By.id("fancybox-content"), AddToCollectionForm.class);
    }

    /**
     * @since 5.9.3
     */
    public AddAllToCollectionForm getAddAllToCollectionPopup() {
        Locator.waitUntilGivenFunctionIgnoring(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return StringUtils.isBlank(
                        driver.findElement(By.id(ADD_ALL_TO_COLLECTION_ACTION_ID)).getAttribute("disabled"));
            }
        }, StaleElementReferenceException.class);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        driver.findElement(By.id(ADD_ALL_TO_COLLECTION_ACTION_ID)).click();
        arm.end();
        Locator.waitUntilElementPresent(By.id("fancybox-content"));
        return getWebFragment(By.id("fancybox-content"), AddAllToCollectionForm.class);
    }

    public boolean isAddToCollectionUpperActionAvailable() {
        try {
            driver.findElement(By.id(ADD_TO_COLLECTION_UPPER_ACTION_ID));
            return true;
        } catch (final NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @since 5.9.3
     */
    public void popupUserMenuActions() {
        userMenuActions.findElement(By.id("nxw_userMenuActions_dropDownMenu")).click();
        Locator.waitUntilGivenFunctionIgnoring(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return !userMenuActions.findElement(By.xpath("//ul[@class='actionSubList']"))
                                       .getAttribute("style")
                                       .equals("display: none;");
            }
        }, StaleElementReferenceException.class);
    }

    /**
     * @since 5.9.3
     */
    public DocumentBasePage switchToPersonalWorkspace() {
        popupUserMenuActions();
        driver.findElement(By.linkText("Personal Workspace")).click();
        return asPage(DocumentBasePage.class);
    }

    /**
     * @since 5.9.3
     */
    public DocumentBasePage switchToDocumentBase() {
        popupUserMenuActions();
        driver.findElement(By.linkText("Back to Document Base")).click();
        return asPage(DocumentBasePage.class);
    }

    /**
     * @since 5.9.3
     */
    public HomePage goToHomePage() {
        homePageLink.click();
        return asPage(HomePage.class);
    }

    /**
     * @since 6.0
     */
    public SearchPage goToSearchPage() {
        searchPageLink.click();
        return asPage(SearchPage.class);
    }

    /**
     * @since 7.3
     */
    public DocumentBasePage goToWorkspaces() {
        documentManagementLink.click();
        return asPage(DocumentBasePage.class);
    }

    /**
     * Returns true if given element representing a main tab is selected in UI.
     *
     * @since 7.3
     */
    public boolean isMainTabSelected(WebElement tab) {
        WebElement elt = Locator.findParentTag(tab, "li");
        String css = elt.getAttribute("class");
        if (css != null && css.contains("selected")) {
            return true;
        }
        return false;
    }

    /**
     * Creates a Workspace from this page.
     *
     * @param workspaceTitle the workspace title
     * @param workspaceDescription the workspace description
     * @return the created Workspace page
     * @since 8.2
     */
    public DocumentBasePage createWorkspace(String workspaceTitle, String workspaceDescription) {
        // Go to Workspaces
        DocumentBasePage workspacesPage = getNavigationSubPage().goToDocument(WORKSPACES_TITLE);
        // Get Workspace creation form page
        WorkspaceCreationFormPage workspaceCreationFormPage = workspacesPage.getWorkspacesContentTab().getWorkspaceCreatePage();
        // Create Workspace
        DocumentBasePage workspacePage = workspaceCreationFormPage.createNewWorkspace(workspaceTitle,
                workspaceDescription);
        return workspacePage;
    }

    /**
     * Deletes the Workspace with title {@code workspaceTitle} from this page.
     *
     * @param workspaceTitle the workspace title
     * @since 8.2
     */
    public void deleteWorkspace(String workspaceTitle) {
        // Go to Workspaces
        DocumentBasePage workspacesPage = getNavigationSubPage().goToDocument(WORKSPACES_TITLE);
        // Delete the Workspace
        workspacesPage.getContentTab().removeDocument(workspaceTitle);
    }

    /**
     * Creates a File from this page.
     *
     * @param fileTitle the file title
     * @param fileDescription the file description
     * @param uploadBlob true if a blob needs to be uploaded (temporary file created for this purpose)
     * @param filePrefix the file prefix
     * @param fileSuffix the file suffix
     * @param fileContent the file content
     * @return the created File page
     * @throws IOException if temporary file creation fails
     * @since 8.2
     */
    public FileDocumentBasePage createFile(String fileTitle, String fileDescription, boolean uploadBlob,
            String filePrefix, String fileSuffix, String fileContent) throws IOException {
        // Get File creation form page
        FileCreationFormPage fileCreationFormPage = getContentTab().getDocumentCreatePage(FILE_TYPE,
                FileCreationFormPage.class);
        // Create File
        FileDocumentBasePage filePage = fileCreationFormPage.createFileDocument(fileTitle, fileDescription, uploadBlob,
                filePrefix, fileSuffix, fileDescription);
        return filePage;
    }

    /**
     * Creates a Collections container from this page.
     *
     * @param collectionsTitle the Collections container title
     * @param fileDescription the collections description
     * @return the created Collections page
     * @since 8.2
     */
    public DocumentBasePage createCollections(String collectionsTitle, String fileDescription) {
        DublinCoreCreationDocumentFormPage dublinCoreDocumentFormPage = getContentTab().getDocumentCreatePage(
                "Collections", DublinCoreCreationDocumentFormPage.class);
        // Create File
        DocumentBasePage documentBasePage = dublinCoreDocumentFormPage.createDocument(collectionsTitle,
                fileDescription);
        return documentBasePage;
    }

    /**
     * Creates a Collection from this page.
     *
     * @param collectionsTitle the Collections container title
     * @param fileDescription the collection description
     * @return the created Collections page
     * @since 8.2
     */
    public CollectionContentTabSubPage createCollection(String collectionsTitle, String fileDescription) {
        CollectionCreationFormPage collectionCreationFormPage = getContentTab().getDocumentCreatePage("Collection",
                CollectionCreationFormPage.class);
        // Create File
        CollectionContentTabSubPage documentBasePage = collectionCreationFormPage.createDocument(collectionsTitle,
                fileDescription);
        return documentBasePage;
    }

    /**
     * Creates a Note from this page.
     *
     * @param noteTitle the note title
     * @param noteDescription the note description
     * @param defineNote true if the content of the note needs to be defined
     * @param noteContent the content of the note
     * @return the created note page.
     * @throws IOException
     * @since 8.2
     */
    public NoteDocumentBasePage createNote(String noteTitle, String noteDescription, boolean defineNote,
            String noteContent) throws IOException {
        // Get the Note creation form
        NoteCreationFormPage noteCreationPage = getContentTab().getDocumentCreatePage(NOTE_TYPE,
                NoteCreationFormPage.class);
        // Create a Note
        NoteDocumentBasePage notePage = noteCreationPage.createNoteDocument(noteTitle, noteDescription, defineNote,
                noteContent);
        return notePage;
    }

}

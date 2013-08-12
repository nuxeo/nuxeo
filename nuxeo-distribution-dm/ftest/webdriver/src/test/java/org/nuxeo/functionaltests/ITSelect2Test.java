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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.AccessRightsSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * Select2 feature test.
 *
 * @since 5.7.3
 */
public class ITSelect2Test extends AbstractTest {

    private final static String USERNAME = "jdoe";

    private final static String PASSWORD = "test";

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    public final static String[] SUBJECTS = { "Comics", "Religion", "Education" };

    public final static String COVERAGE = "France";

    private final static String S2_SINGLE_INPUT_XPATH = "//*[@id='select2-drop']/div/input";

    private final static String S2_MULTIPLE_INPUT_XPATH = "//*[@id='s2id_autogen2']";

    private static final String S2_2LEVELS_SUGGEST_RESULT_XPATH = "//*[@id='select2-drop']/ul/li/ul/li/div";

    private static final String S2_CSS_ACTIVE_CLASS = "select2-active";

    public static final String S2_COVERAGE_FIELD_XPATH = "//*[@id='s2id_document_edit:nxl_dublincore:nxw_coverage_select2']/a/span";

    private static Function<WebDriver, Boolean> S2_MULTIPLE_WAIT_FUNCTION = new Function<WebDriver, Boolean>() {
        public Boolean apply(WebDriver driver) {
            WebElement searchInput = driver.findElement(By.xpath(S2_MULTIPLE_INPUT_XPATH));
            return !searchInput.getAttribute("class").contains(
                    S2_CSS_ACTIVE_CLASS);
        }
    };

    private static Function<WebDriver, Boolean> S2_SINGLE_WAIT_FUNCTION = new Function<WebDriver, Boolean>() {
        public Boolean apply(WebDriver driver) {
            WebElement searchInput = driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH));
            return !searchInput.getAttribute("class").contains(
                    S2_CSS_ACTIVE_CLASS);
        }
    };

    private static Function<WebDriver, Boolean> S2_2LEVELS_SUGGESTION_COMPLETE_WAIT = new Function<WebDriver, Boolean>() {
        public Boolean apply(WebDriver driver) {
            List<WebElement> webElts = driver.findElements(By.xpath(S2_2LEVELS_SUGGEST_RESULT_XPATH));
            return webElts.size() == 1;
        }
    };

    public static void editCoverage(String coverage) {
        addValueToSelect2Field(
                coverage,
                By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_coverage_select2']/a"),
                By.xpath(S2_SINGLE_INPUT_XPATH), S2_SINGLE_WAIT_FUNCTION,
                S2_2LEVELS_SUGGESTION_COMPLETE_WAIT);
    }

    private static void addValueToSelect2Field(String value, By byField,
            By byInput, Function<WebDriver, Boolean> waitLoading,
            Function<WebDriver, Boolean> waitSuggestionComplete) {
        WebElement select2Field = driver.findElement(byField);
        select2Field.click();

        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(3,
                TimeUnit.SECONDS).pollingEvery(100, TimeUnit.MILLISECONDS).ignoring(
                NoSuchElementException.class);

        // wait till the list is loaded
        wait.until(waitLoading);

        WebElement suggestInput = driver.findElement(byInput);

        for (char c : value.toCharArray()) {
            suggestInput.sendKeys(c + "");
            wait.until(waitLoading);
        }

        // wait till suggesgtion is complete
        wait.until(waitSuggestionComplete);

        WebElement suggestion = driver.findElement(By.xpath(S2_2LEVELS_SUGGEST_RESULT_XPATH));
        suggestion.click();
    }

    public static void editSubject(String... subjects) {
        for (String subject : subjects) {
            addValueToSelect2Field(
                    subject,
                    By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']"),
                    By.xpath(S2_MULTIPLE_INPUT_XPATH),
                    S2_MULTIPLE_WAIT_FUNCTION,
                    S2_2LEVELS_SUGGESTION_COMPLETE_WAIT);
        }
    }

    /**
     * Delete created user and data.
     *
     * @throws UserNotConnectedException
     *
     * @since 5.7.3
     */
    private void restoreSate() throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(USERNAME);
        usersTab = usersTab.viewUser(USERNAME).deleteUser();
        DocumentBasePage documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);
        logout();
    }

    /**
     * Create a file document and manipulate coverage and subjects fields based
     * on select2 attributes.
     *
     * @throws Exception
     *
     * @since 5.7.3
     */
    @Test
    public void testSelect2Edit() throws Exception {
        DocumentBasePage documentBasePage;

        DocumentBasePage s = login();

        // Create a new user if not exist
        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = s.getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(USERNAME);
        if (!usersTab.isUserFound(USERNAME)) {
            page = usersTab.getUserCreatePage().createUser(USERNAME, USERNAME,
                    "lastname1", "company1", "email1", PASSWORD, "members");
            usersTab = page.getUsersTab(true);
        } // search user usersTab =
        usersTab.searchUser(USERNAME);
        assertTrue(usersTab.isUserFound(USERNAME));

        // create a new wokspace and grant all rights to the test user
        documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);
        AccessRightsSubPage accessRightSubTab = workspacePage.getManageTab().getAccessRightsSubTab();
        // Need WriteSecurity (so in practice Manage everything) to edit a
        // Workspace
        if (!accessRightSubTab.hasPermissionForUser("Manage everything",
                USERNAME)) {
            accessRightSubTab.addPermissionForUser(USERNAME,
                    "Manage everything", true);
        }

        logout();

        // Log as test user and edit the created workdspace
        documentBasePage = login(USERNAME, PASSWORD).getContentTab().goToDocument(
                "Workspaces").getContentTab().goToDocument(WORKSPACE_TITLE);

        // Create test File
        FileDocumentBasePage filePage = createFile(workspacePage, "Test file",
                "Test File description", false, null, null, null);
        EditTabSubPage editTabSubPage = filePage.getEditTab();

        editCoverage(COVERAGE);

        editSubject(SUBJECTS);

        editTabSubPage.save();

        editTabSubPage = filePage.getEditTab();

        WebElement savedCoverage = driver.findElement(By.xpath(S2_COVERAGE_FIELD_XPATH));
        assertTrue(savedCoverage.getText() != null);
        assertTrue(savedCoverage.getText().equals(COVERAGE));

        List<WebElement> savedSubjects = driver.findElements(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']/ul/li/div"));
        assertEquals(savedSubjects.size(), SUBJECTS.length);

        // Remove the second subject
        WebElement deleteSecondSubjectAction = driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']/ul/li[2]/a"));
        deleteSecondSubjectAction.click();

        // We need to do this because select2 take a little while to write in
        // the form that an entry has been deleted
        Thread.sleep(250);

        editTabSubPage.save();

        editTabSubPage = filePage.getEditTab();

        // Make sure we have one subject removed
        savedSubjects = driver.findElements(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']/ul/li/div"));
        assertEquals(savedSubjects.size(), SUBJECTS.length - 1);

        logout();

        restoreSate();
    }

}

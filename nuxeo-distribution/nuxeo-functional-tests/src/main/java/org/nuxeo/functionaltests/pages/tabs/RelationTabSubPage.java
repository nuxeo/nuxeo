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
 *      <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 *      Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.tabs;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * Representation of a Relations tab page.
 *
 * @since 5.9.1
 */
public class RelationTabSubPage extends DocumentBasePage {

    private static final Log log = LogFactory.getLog(RelationTabSubPage.class);

    private static final int CREATE_FORM_LOADING_TIMEOUT = 20;

    private static final int SELECT2_CHANGE_TIMEOUT = 4;

    private static final String OBJECT_DOCUMENT_UID_ID = "createForm:objectDocumentUid";

    private static final String SELECT2_DOCUMENT_XPATH = "//*[@id='s2id_createForm:nxw_singleDocumentSuggestion_2_select2']";

    // moved @Required on this element to allow read only view
    @Required
    @FindBy(xpath = "//div[@id='nxw_documentTabs_tab_content']/div")
    WebElement messageContainer;

    @FindBy(linkText = "Add a New Relation")
    WebElement addANewRelationLink;

    @FindBy(id = "createForm")
    WebElement createRelationForm;

    @FindBy(xpath = "//*[@id='createForm']/table/tbody/tr[4]/td[2]/input")
    WebElement addButton;

    @FindBy(id = "createForm:predicateUri")
    WebElement predicate;

    @FindBy(name = "createForm:objectType")
    List<WebElement> objectCheckBoxList;

    @FindBy(xpath = "//*[@id='document_relations']/table/tbody/tr")
    List<WebElement> existingRelations;

    /**
     * @param driver
     */
    public RelationTabSubPage(WebDriver driver) {
        super(driver);
    }

    public RelationTabSubPage deleteRelation(int index) {
        getExistingRelations().get(index).findElement(By.linkText("Delete")).click();
        return asPage(RelationTabSubPage.class);
    }

    public List<WebElement> getExistingRelations() {
        return existingRelations;
    }

    public RelationTabSubPage initRelationSetUp() {
        addANewRelationLink.click();

        Function<WebDriver, Boolean> createRelationFormVisible = new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return createRelationForm != null;
            }
        };

        Wait<WebDriver> wait = new FluentWait<>(driver)
                                                                .withTimeout(CREATE_FORM_LOADING_TIMEOUT,
                                                                        TimeUnit.SECONDS)
                                                                .pollingEvery(100, TimeUnit.MILLISECONDS)
                                                                .ignoring(NoSuchElementException.class);

        wait.until(createRelationFormVisible);

        return asPage(RelationTabSubPage.class);
    }

    private boolean isObjectChecked(int index) {
        assert(index < 3 && index >= 0);
        org.junit.Assert.assertNotNull(objectCheckBoxList);
        org.junit.Assert.assertEquals(3, objectCheckBoxList.size());

        return objectCheckBoxList.get(index).isSelected();
    }

    public boolean isObjectDocumentChecked() {
        return isObjectChecked(2);
    }

    public RelationTabSubPage setRelationWithDocument(String documentName, String predicateUri) {

        org.junit.Assert.assertFalse(isObjectDocumentChecked());

        Select predicateSelect = new Select(predicate);
        predicateSelect.selectByValue(predicateUri);

        Select2WidgetElement documentSuggestionWidget = new Select2WidgetElement(driver,
                driver.findElement(By.xpath(SELECT2_DOCUMENT_XPATH)));

        documentSuggestionWidget.selectValue(documentName);

        Function<WebDriver, Boolean> isDocumentSelected = new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                WebElement selectedDocument = driver.findElement(By.id(OBJECT_DOCUMENT_UID_ID));
                String value = selectedDocument.getAttribute("value");
                boolean result = StringUtils.isNotBlank(value);
                if (!result) {
                    log.debug("Waiting for select2 ajaxReRender");
                }
                return result;
            }
        };

        org.junit.Assert.assertTrue(isObjectDocumentChecked());

        Wait<WebDriver> wait = new FluentWait<>(driver).withTimeout(SELECT2_CHANGE_TIMEOUT, TimeUnit.SECONDS)
                                                                .pollingEvery(100, TimeUnit.MILLISECONDS)
                                                                .ignoring(StaleElementReferenceException.class);

        wait.until(isDocumentSelected);

        if (log.isDebugEnabled()) {
            WebElement selectedDocument = driver.findElement(By.id(OBJECT_DOCUMENT_UID_ID));
            log.debug("Submitting relation on document: " + selectedDocument.getAttribute("value"));
        }

        addButton.click();

        return asPage(RelationTabSubPage.class);
    }

    /**
     * @since 8.3
     */
    @Override
    public boolean hasNewRelationLink() {
        try {
            return addANewRelationLink.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}

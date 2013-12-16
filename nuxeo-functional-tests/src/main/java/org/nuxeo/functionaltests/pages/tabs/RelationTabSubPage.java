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
 *      <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.pages.tabs;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
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

    @Required
    @FindBy(linkText = "Add a new relation")
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

    @FindBy(id = "createForm:objectDocumentUid")
    WebElement selectedDocument;

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
            public Boolean apply(WebDriver driver) {
                return createRelationForm != null;
            }
        };

        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(
                CREATE_FORM_LOADING_TIMEOUT, TimeUnit.SECONDS).pollingEvery(
                100, TimeUnit.MILLISECONDS).ignoring(
                NoSuchElementException.class);

        wait.until(createRelationFormVisible);

        return asPage(RelationTabSubPage.class);
    }

    private boolean isObjectChecked(int index) {
        assert (index < 3 && index >= 0);
        org.junit.Assert.assertNotNull(objectCheckBoxList);
        org.junit.Assert.assertEquals(3, objectCheckBoxList.size());

        return objectCheckBoxList.get(index).isSelected();
    }

    public boolean isObjectDocumentChecked() {
        return isObjectChecked(2);
    }

    public RelationTabSubPage setRelationWithDocument(String documentName,
            String predicateUri) {

        org.junit.Assert.assertFalse(isObjectDocumentChecked());

        Select predicateSelect = new Select(predicate);
        predicateSelect.selectByValue(predicateUri);

        Select2WidgetElement documentSuggestionWidget = new Select2WidgetElement(
                driver,
                By.xpath("//*[@id='s2id_createForm:nxw_singleDocumentSuggestion_select2']"));

        documentSuggestionWidget.selectValue(documentName);

        Function<WebDriver, Boolean> isDocumentSelected = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                String value = selectedDocument.getAttribute("value");
                boolean result = StringUtils.isNotBlank(value);
                if (!result) {
                    log.warn("Waiting for select2 ajaxReRender");
                }
                return result;
            }
        };

        org.junit.Assert.assertTrue(isObjectDocumentChecked());

        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(
                SELECT2_CHANGE_TIMEOUT, TimeUnit.SECONDS).pollingEvery(100,
                TimeUnit.MILLISECONDS);

        wait.until(isDocumentSelected);

        log.warn("Submitting relation on document: "
                + selectedDocument.getAttribute("value"));

        addButton.click();

        return asPage(RelationTabSubPage.class);
    }

}

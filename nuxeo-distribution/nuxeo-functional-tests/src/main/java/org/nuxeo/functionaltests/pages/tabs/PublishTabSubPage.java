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
 *     Gabriel Barata
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.tabs;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.3
 */
public class PublishTabSubPage extends AbstractPage {

    @FindBy(xpath = "//select[@id='publishTreeForm:publishSelectTreeName']")
    public WebElement selectTree;

    @FindBy(xpath = "//select[contains(@id, 'publishTreeForm:j')]")
    public WebElement selectRendition;

    @FindBy(xpath = "//div[@id='publishTreeForm:publishingInfoList']/table/tbody/tr")
    List<WebElement> publishingInfos;

    public PublishTabSubPage(WebDriver driver) {
        super(driver);
    }

    public PublishTabSubPage publish(String sectionTree, String rendtion, String sectionName) {
        selectItemInDropDownMenu(selectTree, sectionTree);
        selectItemInDropDownMenu(selectRendition, rendtion);

        int index = findTreeNodeIndex(sectionName);
        publishIemInPublishTreeForm(index);

        return asPage(PublishTabSubPage.class);
    }

    public List<WebElement> getPublishingInfos() {
        return publishingInfos;
    }

    public PublishTabSubPage unpublish(String sectionName, String version) {

        for (WebElement publishRow : getPublishingInfos()) {
            if ((StringUtils.isBlank(sectionName) || matchSectionName(publishRow, sectionName))
                    && (StringUtils.isBlank(version) || matchVersion(publishRow, version))) {
                publishRow.findElement(By.linkText("Unpublish")).click();
                return asPage(PublishTabSubPage.class);
            }
        }

        return this;
    }

    private boolean matchSectionName(WebElement publishRow, String sectionName) {
        String sectionNameXpath = String.format("./td/a[contains(text(),'%s')]", sectionName);
        try {
            return publishRow.findElement(By.xpath(sectionNameXpath)) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean matchVersion(WebElement publishRow, String version) {
        String versionXpath = String.format("./td[text()='%s']", version);
        try {
            return publishRow.findElement(By.xpath(versionXpath)) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private int findTreeNodeIndex(String itemName) {
        List<WebElement> elements = getTreeNode();
        int index = 0;
        for (WebElement sub : elements) {
            if (sub.getText().equals(itemName)) {
                return index - 1; // skip index for "Sections"
            }
            index++;
        }
        List<WebElement> expanders = getItemExpanderInPublishTreeForm();
        if (expanders.size() == 0) {
            return -1;
        } else {
            for (WebElement expander : expanders) {
                Locator.waitUntilEnabledAndClick(expander);
            }
            return findTreeNodeIndex(itemName);
        }
    }

    private List<WebElement> getTreeNode() {
        return findElementsWithTimeout(By.xpath(
                "//div[@id='publishTreeForm:sectionTree'] //div[@class='rf-trn'] //span[@class='tipsyShow tipsyGravityS']"));
    }

    private List<WebElement> getItemExpanderInPublishTreeForm() {
        return findElementsWithTimeout(
                By.xpath("//div[@id='publishTreeForm:sectionTree'] //span[@class='rf-trn-hnd-colps rf-trn-hnd']"));
    }

    private void publishIemInPublishTreeForm(int index) {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        findElementWaitUntilEnabledAndClick(
                By.xpath("//div[@id='publishTreeForm:sectionTree'] //a[contains(@id, 'publishRecursiveAdaptor." + index
                        + ":publishCommandLink')]"));
        arm.end();
    }
}

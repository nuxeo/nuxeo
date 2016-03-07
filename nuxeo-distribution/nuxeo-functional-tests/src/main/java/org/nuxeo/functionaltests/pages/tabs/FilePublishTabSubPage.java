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
 */
package org.nuxeo.functionaltests.pages.tabs;

import java.util.List;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.2
 */
public class FilePublishTabSubPage extends AbstractPage {

    @FindBy(xpath = "//select[@id='publishTreeForm:publishSelectTreeName']")
    public WebElement selectTree;

    @FindBy(xpath = "//select[contains(@id, 'publishTreeForm:j')]")
    public WebElement selectRendition;

    public FilePublishTabSubPage(WebDriver driver) {
        super(driver);
    }

    public FilePublishTabSubPage publish(String sectionTree, String rendtion, String sectionName) {
        selectItemInDropDownMenu(selectTree, sectionTree);
        selectItemInDropDownMenu(selectRendition, rendtion);

        int index = findTreeNodeIndex(sectionName);
        publishIemInPublishTreeForm(index);

        return asPage(FilePublishTabSubPage.class);
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
                expander.click();
            }
            return findTreeNodeIndex(itemName);
        }
    }

    private List<WebElement> getTreeNode() {
        return findElementsWithTimeout(By.xpath("//div[@id='publishTreeForm:sectionTree'] //div[@class='rf-trn'] //span[@class='tipsyShow tipsyGravityS']"));
    }

    private List<WebElement> getItemExpanderInPublishTreeForm() {
        return findElementsWithTimeout(By.xpath("//div[@id='publishTreeForm:sectionTree'] //span[@class='rf-trn-hnd-colps rf-trn-hnd']"));
    }

    private void publishIemInPublishTreeForm(int index) {
        findElementWaitUntilEnabledAndClick(By.xpath("//div[@id='publishTreeForm:sectionTree'] //a[contains(@id, 'publishRecursiveAdaptor."
                + index + ":publishCommandLink')]"));
    }
}

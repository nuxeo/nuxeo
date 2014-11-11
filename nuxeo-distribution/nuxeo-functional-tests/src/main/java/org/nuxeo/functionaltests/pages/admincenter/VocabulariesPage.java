/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages.admincenter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class VocabulariesPage extends AbstractPage {

    @FindBy(id = "selectDirectoryForm:directoriesList")
    WebElement directoriesListSelect;

    public VocabulariesPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Return the list of directories in the select box
     *
     * @return
     */
    public List<String> getDirectoriesList() {
        ArrayList<String> directoryList = new ArrayList();
        List<WebElement> list = directoriesListSelect.findElements(By.tagName("option"));
        for (WebElement webElement : list) {
            directoryList.add(webElement.getText());
        }
        return directoryList;
    }

    /**
     * Select one of the directory in the select box
     *
     * @param directoryName
     */
    public void select(String directoryName) {
        List<WebElement> list = directoriesListSelect.findElements(By.tagName("option"));
        for (WebElement webElement : list) {
            if (directoryName.trim().equals(webElement.getText().trim())) {
                webElement.setSelected();
                break;
            }
        }
    }

}

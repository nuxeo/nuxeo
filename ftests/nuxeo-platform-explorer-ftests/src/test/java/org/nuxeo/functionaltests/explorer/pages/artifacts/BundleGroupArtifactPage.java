/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.pages.artifacts;

import static org.junit.Assert.fail;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class BundleGroupArtifactPage extends ArtifactPage {

    @FindBy(xpath = "//ul[@class='subbroups']")
    public WebElement subgroups;

    @FindBy(xpath = "//ul[@class='groupbundles']")
    public WebElement bundles;

    public BundleGroupArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference(boolean partial, boolean legacy) {
        if (partial) {
            String title = "Bundle group my-partial-server";
            if (legacy) {
                title = "Bundle group apidoc";
            }
            checkCommon(title, title, null, "Bundles");
            checkDocumentationText(null);
            checkSubGroup(null);
            checkBundle("org.nuxeo.apidoc.core");
            checkBundle("org.nuxeo.apidoc.repo");
            checkBundle("org.nuxeo.apidoc.webengine");
        } else {
            checkCommon("Bundle group org.nuxeo.ecm.platform", "Bundle group org.nuxeo.ecm.platform", null,
                    "Bundle Subgroups\n" + "Bundles");
            checkDocumentationText(null);
            checkSubGroup("org.nuxeo.ecm.platform.comment");
            checkSubGroup("org.nuxeo.ecm.platform.filemanager");
            checkBundle("org.nuxeo.ecm.platform.api");
        }
    }

    @Override
    public void checkAlternative() {
        checkCommon("Bundle group org.nuxeo.ecm.directory", "Bundle group org.nuxeo.ecm.directory", null, "Bundles");
        checkDocumentationText(null);
        checkSubGroup(null);
        checkBundle("org.nuxeo.ecm.directory");
        checkBundle("org.nuxeo.ecm.directory.api");
    }

    @Override
    protected void checkSelectedTab() {
        // NOOP
    }

    public void checkSubGroup(String id) {
        if (id == null) {
            try {
                subgroups.getText();
                fail("no subgroups should be found");
            } catch (NoSuchElementException e) {
                // ok
            }
        } else {
            subgroups.findElement(By.linkText(id));
        }
    }

    public void checkBundle(String id) {
        bundles.findElement(By.linkText(id));
    }

}

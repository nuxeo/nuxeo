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

import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 11.1
 */
public class ExtensionPointArtifactPage extends ArtifactPage {

    public ExtensionPointArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference() {
        checkCommon("Extension point org.nuxeo.apidoc.snapshot.SnapshotManagerComponent--plugins",
                "Extension point plugins", "In component org.nuxeo.apidoc.snapshot.SnapshotManagerComponent");
    }

    @Override
    public void checkAlternative() {
        checkCommon("Extension point org.nuxeo.ecm.core.schema.TypeService--doctype", "Extension point doctype",
                "In component org.nuxeo.ecm.core.schema.TypeService");
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.extensionPoints);
    }

    public void generateOverride(String contributionId) {
        WebElement li = driver.findElement(By.id(contributionId));
        clickOn(li.findElement(By.className("override")));
    }

}

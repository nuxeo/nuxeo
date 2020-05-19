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

import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class ServiceArtifactPage extends ArtifactPage {

    @Required
    @FindBy(xpath = "//div[@class='implementation']")
    public WebElement implementation;

    @Required
    @FindBy(xpath = "//div[@class='implementation']//a[@class='javadoc']")
    public WebElement javadocLink;

    public ServiceArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference(boolean partial, boolean legacy) {
        checkCommon("Service org.nuxeo.apidoc.snapshot.SnapshotManager",
                "Service org.nuxeo.apidoc.snapshot.SnapshotManager",
                "In component org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", null);
        checkDocumentationText(null);
        checkImplementationText("Javadoc: org.nuxeo.apidoc.snapshot.SnapshotManager");
        checkJavadocLink("/javadoc/org/nuxeo/apidoc/snapshot/SnapshotManager.html");
    }

    @Override
    public void checkAlternative() {
        checkCommon("Service org.nuxeo.ecm.platform.types.TypeManager",
                "Service org.nuxeo.ecm.platform.types.TypeManager",
                "In component org.nuxeo.ecm.platform.types.TypeService", null);
        checkDocumentationText(null);
        checkImplementationText("Javadoc: org.nuxeo.ecm.platform.types.TypeManager");
        checkJavadocLink("/javadoc/org/nuxeo/ecm/platform/types/TypeManager.html");
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.services);
    }

    public void checkImplementationText(String expected) {
        assertEquals(expected, implementation.getText());
    }

    public void checkJavadocLink(String expected) {
        checkJavadocLink(expected, javadocLink);
    }

}

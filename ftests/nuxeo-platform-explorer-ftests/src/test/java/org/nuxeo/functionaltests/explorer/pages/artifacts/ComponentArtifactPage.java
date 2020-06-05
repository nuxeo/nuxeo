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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class ComponentArtifactPage extends ArtifactPage {

    @FindBy(xpath = "//div[@class='implementation']")
    public WebElement implementation;

    @FindBy(xpath = "//div[@class='implementation']//a[@class='javadoc']")
    public WebElement javadocLink;

    @Required
    @FindBy(xpath = "//div[@id='registrationOrder']")
    public WebElement registrationOrder;

    public ComponentArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference(boolean partial, boolean legacy) {
        String toc = "Documentation\n" + "Registration Order\n" + "Implementation\n" + "Services\n"
                + "Extension Points\n" + "Contributions\n" + "XML Source";
        if (legacy) {
            toc = "Documentation\n" + "Registration Order\n" + "Implementation\n" + "Services\n" + "Extension Points\n"
                    + "XML Source";
        }
        checkCommon("Component org.nuxeo.apidoc.snapshot.SnapshotManagerComponent",
                "Component org.nuxeo.apidoc.snapshot.SnapshotManagerComponent", "In bundle org.nuxeo.apidoc.repo", toc);
        checkRequirements(null);
        checkDocumentationText(
                "This component handles the introspection of the current live Runtime as a distribution.\n" //
                        + "It can also persist this introspection as Nuxeo documents, to handle import and export of external distributions.");
        checkImplementationText("Javadoc: org.nuxeo.apidoc.snapshot.SnapshotManagerComponent");
        checkJavadocLink("/javadoc/org/nuxeo/apidoc/snapshot/SnapshotManagerComponent.html");
        checkRegistrationOrder(!legacy);
    }

    @Override
    public void checkAlternative() {
        checkCommon("Component org.nuxeo.ecm.automation.server.marshallers",
                "Component org.nuxeo.ecm.automation.server.marshallers", "In bundle org.nuxeo.ecm.automation.io",
                "Requirements\n" + "Registration Order\n" + "Contributions\n" + "XML Source");
        checkRequirements(List.of("org.nuxeo.ecm.platform.contentview.json.marshallers"));
        checkDocumentationText(null);
        checkImplementationText(null);
        checkJavadocLink(null);
        checkRegistrationOrder(true);
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.components);
    }

    public void checkImplementationText(String expected) {
        checkTextIfExists(expected, implementation);
    }

    public void checkJavadocLink(String expected) {
        checkLink(expected, javadocLink);
    }

    public void checkRegistrationOrder(boolean set) {
        assertEquals(!set, StringUtils.isBlank(registrationOrder.getText()));
    }

}

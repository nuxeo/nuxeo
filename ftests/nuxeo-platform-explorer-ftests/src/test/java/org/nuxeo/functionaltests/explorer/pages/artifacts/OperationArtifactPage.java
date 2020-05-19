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
public class OperationArtifactPage extends ArtifactPage {

    @FindBy(xpath = "//div[@class='description']")
    public WebElement opDescription;

    @Required
    @FindBy(xpath = "//div[@class='info']")
    public WebElement info;

    @Required
    @FindBy(xpath = "//div[@class='parameters']")
    public WebElement parameters;

    @Required
    @FindBy(xpath = "//div[@class='signature']")
    public WebElement signature;

    @Required
    @FindBy(xpath = "//div[@class='implementation']")
    public WebElement implementation;

    @FindBy(xpath = "//div[@class='implementation']//a[@class='javadoc']")
    public WebElement javadocLink;

    @FindBy(xpath = "//div[@class='json']")
    public WebElement json;

    public OperationArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference(boolean partial, boolean legacy) {
        checkCommon("Operation Document.AddFacet", "Operation Document.AddFacet (Add Facet)",
                "In component org.nuxeo.ecm.core.automation.coreContrib",
                "Description\n" + "Parameters\n" + "Signature\n" + "Implementation Information\n" + "JSON Definition");
        checkOperationDescriptionText("Adds the facet to the document.\n" //
                + "WARNING: The save parameter is true by default, which means the document is saved in "
                + "the database after adding the facet. It must be set to false when the operation is used "
                + "in the context of an event that will fail if the document is saved (empty document created, "
                + "about to create, before modification, ...).");
        checkInfoText("Operation id Document.AddFacet\n" //
                + "Aliases Document.AddFacet\n" //
                + "Category Document\n" //
                + "Label Add Facet\n" //
                + "Requires\n" //
                + "Since");
        checkParametersText("Name Description Type Required Default value\n" //
                + "facet string yes  \n" //
                + "save boolean no true ");
        checkSignatureText("Inputs document, documents\n" //
                + "Outputs document, documents");
        checkImplementationText(
                "Implementation Class Javadoc: org.nuxeo.ecm.automation.core.operations.document.AddFacet\n" //
                        + "Contributing Component org.nuxeo.ecm.core.automation.coreContrib");
        checkJavadocLink("/javadoc/org/nuxeo/ecm/automation/core/operations/document/AddFacet.html");
        checkJsonText("{\n" //
                + "  \"id\" : \"Document.AddFacet\",\n" //
                + "  \"aliases\" : [ \"Document.AddFacet\" ],\n" //
                + "  \"label\" : \"Add Facet\",\n" //
                + "  \"category\" : \"Document\",\n" //
                + "  \"requires\" : null,\n" //
                + "  \"description\" : \"Adds the facet to the document. <p>WARNING: The save parameter is true by default, which means the document is saved in the database after adding the facet. It must be set to false when the operation is used in the context of an event that will fail if the document is saved (empty document created, about to create, before modification, ...).</p>\",\n" //
                + "  \"url\" : \"Document.AddFacet\",\n" //
                + "  \"signature\" : [ \"document\", \"document\", \"documents\", \"documents\" ],\n" //
                + "  \"params\" : [ {\n" //
                + "    \"name\" : \"facet\",\n" //
                + "    \"description\" : \"\",\n" //
                + "    \"type\" : \"string\",\n" //
                + "    \"required\" : true,\n" //
                + "    \"widget\" : null,\n" //
                + "    \"order\" : 0,\n" //
                + "    \"values\" : [ ]\n" //
                + "  }, {\n" //
                + "    \"name\" : \"save\",\n" //
                + "    \"description\" : \"\",\n" //
                + "    \"type\" : \"boolean\",\n" //
                + "    \"required\" : false,\n" //
                + "    \"widget\" : null,\n" //
                + "    \"order\" : 0,\n" //
                + "    \"values\" : [ \"true\" ]\n" //
                + "  } ]\n" //
                + "}");
    }

    @Override
    public void checkAlternative() {
        checkCommon("Operation FileManager.ImportWithMetaData",
                "Operation FileManager.ImportWithMetaData (FileManager.ImportWithMetaData)",
                // Non-regression test for NXP-29025 as this previously stated "In component BuiltIn" for all chains
                "In component org.nuxeo.ecm.core.automation.features.operations",
                "Parameters\n" + "Signature\n" + "Implementation Information\n" + "JSON Definition");
        checkImplementationText(
                "Implementation Class Javadoc: org.nuxeo.ecm.automation.core.impl.OperationChainCompiler.CompiledChainImpl\n"//
                        + "Contributing Component org.nuxeo.ecm.core.automation.features.operations");
        checkJavadocLink(
                "/javadoc/org/nuxeo/ecm/automation/core/impl/OperationChainCompiler.CompiledChainImpl.html");
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.operations);
    }

    public void checkOperationDescriptionText(String expected) {
        assertEquals(expected, opDescription.getText());
    }

    public void checkInfoText(String expected) {
        assertEquals(expected, info.getText());
    }

    public void checkParametersText(String expected) {
        assertEquals(expected, parameters.getText());
    }

    public void checkSignatureText(String expected) {
        assertEquals(expected, signature.getText());
    }

    public void checkImplementationText(String expected) {
        assertEquals(expected, implementation.getText());
    }

    public void checkJavadocLink(String expected) {
        checkJavadocLink(expected, javadocLink);
    }

    public void checkJsonText(String expected) {
        assertEquals(expected, json.getText());
    }

}

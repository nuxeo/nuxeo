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
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class ContributionArtifactPage extends ArtifactPage {

    @FindBy(xpath = "//ul[@class='block-list']")
    public WebElement contributionsList;

    public ContributionArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference() {
        checkCommon("Contribution org.nuxeo.apidoc.adapterContrib--adapters",
                "Contribution org.nuxeo.apidoc.adapterContrib--adapters",
                "In component org.nuxeo.apidoc.adapterContrib",
                "Documentation\n" + "Extension Point\n" + "Contributed Items\n" + "XML Source");
        checkDocumentationText("These contributions provide a mapping between live introspections "
                + "and persisted representations of a distribution.");
    }

    @Override
    public void checkAlternative() {
        checkCommon("Contribution org.nuxeo.apidoc.listener.contrib--listener",
                "Contribution org.nuxeo.apidoc.listener.contrib--listener",
                "In component org.nuxeo.apidoc.listener.contrib",
                "Documentation\n" + "Extension Point\n" + "Contributed Items\n" + "XML Source");
        checkDocumentationText("These contributions are used for latest distribution flag update "
                + "and XML attributes extractions in extension points.");
        checkContributionItemText(1,
                "<listener async=\"false\" class=\"org.nuxeo.apidoc.listener.LatestDistributionsListener\" name=\"latestDistributionsListener\" postCommit=\"false\">\n" //
                        + "      <documentation>\n" //
                        + "        Updates latest distribution flag.\n" //
                        + "      </documentation>\n" //
                        + "      <event>aboutToCreate</event>\n" //
                        + "      <event>beforeDocumentModification</event>\n" //
                        + "    </listener>\n" //
                        + "listener latestDistributionsListener\n" //
                        + "Updates latest distribution flag.");
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.contributions);
    }

    public void checkContributionItemText(int index, String expected) {
        WebElement element = contributionsList.findElement(By.xpath(".//li[" + index + "]"));
        checkTextIfExists(expected, element);
    }

    public void toggleGenerateOverride() {
        findElementWaitUntilEnabledAndClick(By.id("overrideStart"));
    }

    public void doGenerateOverride() {
        findElementWaitUntilEnabledAndClick(By.id("overrideGen"));
    }

}

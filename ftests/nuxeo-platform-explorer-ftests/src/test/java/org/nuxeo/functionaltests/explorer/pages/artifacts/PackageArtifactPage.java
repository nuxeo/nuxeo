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
import static org.junit.Assert.assertFalse;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class PackageArtifactPage extends ArtifactPage {

    @Required
    @FindBy(xpath = "//td[@id='packageId']")
    public WebElement packageId;

    @Required
    @FindBy(xpath = "//td[@id='packageName']")
    public WebElement packageName;

    @Required
    @FindBy(xpath = "//td[@id='packageVersion']")
    public WebElement packageVersion;

    @FindBy(xpath = "//a[@id='marketplaceLink']")
    public WebElement marketplaceLink;

    @Required
    @FindBy(xpath = "//div[@id='bundles']")
    public WebElement bundles;

    @FindBy(xpath = "//ul[@id='dependencies']")
    public WebElement dependencies;

    @FindBy(xpath = "//ul[@id='optionalDependencies']")
    public WebElement optionalDependencies;

    @FindBy(xpath = "//ul[@id='conflicts']")
    public WebElement conflicts;

    public PackageArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference(boolean partial, boolean legacy) {
        checkCommon("Package Platform Explorer", "Package Platform Explorer (platform-explorer)", null,
                "General Information\n" + "Bundles");
        String version = packageVersion.getText();
        assertFalse(StringUtils.isBlank(version));
        checkPackageId("platform-explorer-" + version);
        checkPackageName("platform-explorer");
        checkMarketplaceLink(
                "https://connect.nuxeo.com/nuxeo/site/marketplace/package/platform-explorer?version=" + version);
        checkBundles("org.nuxeo.apidoc.core\n" + "org.nuxeo.apidoc.repo\n" + "org.nuxeo.apidoc.webengine\n"
                + "org.nuxeo.ecm.webengine.ui");
        checkDependencies(null);
        checkOptionalDependencies(null);
        checkConflicts(null);
    }

    @Override
    public void checkAlternative() {
        // NOOP
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.packages);
    }

    public void checkPackageId(String expected) {
        assertEquals(expected, packageId.getText());
    }

    public void checkPackageName(String expected) {
        assertEquals(expected, packageName.getText());
    }

    public void checkPackageVersion(String expected) {
        assertEquals(expected, packageVersion.getText());
    }

    public void checkMarketplaceLink(String expected) {
        checkLink(expected, marketplaceLink);
    }

    public void checkBundles(String expected) {
        assertEquals(expected, bundles.getText());
    }

    public void checkDependencies(String expected) {
        checkTextIfExists(expected, dependencies);
    }

    public void checkOptionalDependencies(String expected) {
        checkTextIfExists(expected, optionalDependencies);
    }

    public void checkConflicts(String expected) {
        checkTextIfExists(expected, conflicts);
    }

}

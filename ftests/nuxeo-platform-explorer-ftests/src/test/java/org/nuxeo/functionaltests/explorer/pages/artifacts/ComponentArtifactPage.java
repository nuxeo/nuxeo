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

import java.util.List;

import org.nuxeo.functionaltests.explorer.pages.DistributionHeaderFragment;
import org.openqa.selenium.WebDriver;

/**
 * @since 11.1
 */
public class ComponentArtifactPage extends ArtifactPage {

    public ComponentArtifactPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void checkReference() {
        checkCommon("Component org.nuxeo.apidoc.adapterContrib", "Component org.nuxeo.apidoc.adapterContrib",
                "In bundle org.nuxeo.apidoc.repo");
        checkRequirements(null);
    }

    @Override
    public void checkAlternative() {
        checkCommon("Component org.nuxeo.ecm.automation.server.marshallers",
                "Component org.nuxeo.ecm.automation.server.marshallers", "In bundle org.nuxeo.ecm.automation.io");
        checkRequirements(List.of("org.nuxeo.ecm.platform.contentview.json.marshallers"));
    }

    @Override
    public void checkSelectedTab() {
        DistributionHeaderFragment header = asPage(DistributionHeaderFragment.class);
        header.checkSelectedTab(header.components);
    }

}

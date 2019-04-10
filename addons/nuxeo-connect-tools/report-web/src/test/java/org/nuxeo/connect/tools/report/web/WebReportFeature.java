/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.tools.report.web;

import org.nuxeo.connect.tools.report.ReportFeature;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.ecm.webengine.test.WebEngineHomePage;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.web.Browser;
import org.nuxeo.runtime.test.runner.web.BrowserFamily;
import org.nuxeo.runtime.test.runner.web.HomePage;

@Features({ ReportFeature.class, WebEngineFeature.class })
@Deploy("org.nuxeo.connect.tools.report.web")
@Browser(type = BrowserFamily.HTML_UNIT_JS)
@HomePage(type = WebEngineHomePage.class, url = "http://localhost:${PORT}/")
public class WebReportFeature implements RunnerFeature {

    static FeaturesRunner runner;

    @Override
    public void initialize(FeaturesRunner runner) {
        WebReportFeature.runner = runner;
    }


}

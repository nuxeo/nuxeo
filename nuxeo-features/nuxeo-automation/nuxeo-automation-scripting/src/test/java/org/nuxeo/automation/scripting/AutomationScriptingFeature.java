/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.automation.scripting;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;

/**
 *
 *
 * @since 8.4
 */
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features", "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.automation.scripting", "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy({ "org.nuxeo.ecm.automation.scripting.tests:automation-scripting-contrib.xml",
        "org.nuxeo.ecm.automation.scripting:core-types-contrib.xml" })
public class AutomationScriptingFeature extends SimpleFeature {

    @Inject
    AutomationScriptingService scripting;

    FeaturesRunner runner;

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        this.runner = runner;
    }

    InputStream load(String location) throws IOException {
        return runner.getTargetTestResource(location).openStream();
    }

    public <T> T run(String location, CoreSession session, Class<T> typeof) throws Exception {
        try (AutomationScriptingService.Session context = scripting.get(session)) {
            return typeof.cast(context.run(load(location)));
        }
    }

}

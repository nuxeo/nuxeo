/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.platform.test;

import org.nuxeo.ecm.core.api.local.DummyLoginFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Feature deploying the needed bundles to have a full authentication mechanism.
 *
 * @since 11.1
 */
@Features(UserManagerFeature.class)
@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.web.common")
public class NuxeoLoginFeature implements RunnerFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        if (runner.getFeature(DummyLoginFeature.class) != null) {
            // if DummyLoginFeature is present, we want to override its dummy loginAs
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            harness.deployContrib("org.nuxeo.ecm.platform.test", "login-as-config.xml");
        }
    }

}


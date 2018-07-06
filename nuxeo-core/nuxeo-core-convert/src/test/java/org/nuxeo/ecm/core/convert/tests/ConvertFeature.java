/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     ataillefer
 */
package org.nuxeo.ecm.core.convert.tests;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 6.0
 */
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.core.convert.api")
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.core.mimetype")
public class ConvertFeature implements RunnerFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        // we need to deploy it by hand to overwrite the settings deployed at class level
        runner.getFeature(RuntimeFeature.class).getHarness().deployContrib("org.nuxeo.ecm.core.convert",
                "OSGI-INF/convert-service-default-test-config.xml");
    }
}

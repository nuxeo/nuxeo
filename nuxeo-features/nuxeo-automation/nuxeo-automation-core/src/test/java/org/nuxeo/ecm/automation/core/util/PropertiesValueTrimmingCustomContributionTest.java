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
 * Contributors:
 *     Ronan DANIELLOU <rdaniellou@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * With custom contribution.
 *
 * @since 8.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.core" })
public class PropertiesValueTrimmingCustomContributionTest extends PropertiesTestAbstract {

    @Deploy("org.nuxeo.ecm.automation.core:OSGI-INF/test-properties-contrib.xml")
    @Test
    public void loadProperties_value_trimmed_true() throws IOException {
        Boolean isTrimmed = Properties.isPropertyValueTrimmed();
        assertThat(isTrimmed).isTrue();
        loadProperties(isTrimmed);
    }

}

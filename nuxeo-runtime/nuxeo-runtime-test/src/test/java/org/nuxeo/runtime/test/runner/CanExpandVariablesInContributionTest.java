/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.test.runner;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.InlineRef;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class CanExpandVariablesInContributionTest {

    RuntimeService runtime = Framework.getRuntime();

    @Test
    public void variablesAreExpanded() throws Exception {
        RuntimeContext ctx = runtime.getContext();
        System.setProperty("nuxeo.test.domain", "test");
        Framework.getProperties().setProperty("nuxeo.test.contrib", "contrib");
        InlineRef contribRef = new InlineRef("test", "<component name=\"${nuxeo.test.domain}:${nuxeo.test.contrib}\"/>");

        ctx.deploy(contribRef);
        // force components refresh since components are already started in @Before methods
        runtime.getComponentManager().refresh(false);

        ComponentInstance component = runtime.getComponentInstance("test:contrib");
        assertThat("component is installed", component, notNullValue());
        System.clearProperty("nuxeo.test.domain");
    }

}

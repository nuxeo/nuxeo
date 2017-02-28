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
 *     Florent Guillaume
 */
package org.nuxeo.platform.el;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.el.ELContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.el.ELService;
import org.nuxeo.platform.el.DummyELContextFactory.DummyELContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.el")
@LocalDeploy("org.nuxeo.ecm.platform.el.tests:OSGI-INF/test-elcontextfactory-contrib.xml")
public class TestELService {

    @Test
    public void testList() {
        ELService elService = Framework.getService(ELService.class);
        ELContext elContext = elService.createELContext();
        assertNotNull(elContext);
        assertTrue(elContext.getClass().getName(), elContext instanceof DummyELContext);
    }

}

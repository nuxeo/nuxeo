/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class RuntimeInitializationTest {

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    public void testContributions() {
        // do nothing
    }

    protected void checkDupe(boolean detected) {
        List<String> errors = Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR);
        if (detected) {
            assertEquals(List.of("Duplicate component name: service:my.comp2"), errors);
        } else {
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    @Deploy("org.nuxeo.runtime.test.tests:CopyOfMyComp2.xml")
    public void testContributionsWithDuplicateComponent() {
        checkDupe(true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    public void testContributionsWithDuplicateComponentSameFile() {
        // dupe annotation on tests not detected (?)
        checkDupe(false);
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    public void testContributionsWithDuplicateComponentSameFileHotReload() throws Exception {
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:MyComp2.xml");
        checkDupe(true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    public void testContributionsWithDuplicateComponentHotReload() throws Exception {
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CopyOfMyComp2.xml");
        checkDupe(true);
    }

}

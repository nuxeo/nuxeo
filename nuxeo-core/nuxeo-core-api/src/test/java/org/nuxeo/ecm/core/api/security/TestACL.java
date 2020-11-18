/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

@RunWith(FeaturesRunner.class)
@Features(LogCaptureFeature.class)
public class TestACL {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    private ACL acl;

    @Before
    public void setUp() {
        acl = new ACLImpl("test acl");
    }

    @After
    public void tearDown() {
        acl = null;
    }

    @Test
    public void testGetName() {
        assertEquals("test acl", acl.getName());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "DEBUG", loggerClass = ACLImpl.class)
    public void testSetNullName() {
        acl = new ACLImpl(null, false);
        assertEquals(ACL.LOCAL_ACL, acl.getName());

        // Check the message has been logged
        List<String> eventsCaught = logCaptureResult.getCaughtEventMessages();
        assertEquals("ACL name is null", eventsCaught.get(0));
    }

    @Test
    public void testAddingACEs() {
        assertEquals(0, acl.getACEs().length);
        acl.add(new ACE("bogdan", "write", false));
        assertEquals(1, acl.getACEs().length);
    }

}

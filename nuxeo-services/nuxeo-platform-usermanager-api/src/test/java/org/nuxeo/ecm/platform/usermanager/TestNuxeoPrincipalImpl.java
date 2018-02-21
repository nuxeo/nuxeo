/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Florent Guillaume
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.jtajca")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core")
@Deploy("org.nuxeo.ecm.directory.types.contrib")
public class TestNuxeoPrincipalImpl {

    @Test
    public void testEquals() {
        NuxeoPrincipalImpl a = new NuxeoPrincipalImpl("foo");
        NuxeoPrincipalImpl b = new NuxeoPrincipalImpl("foo");
        NuxeoPrincipalImpl c = new NuxeoPrincipalImpl("bar");
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertFalse(a.equals(c));
        assertFalse(c.equals(a));
        assertFalse(a.equals(null));
        assertFalse(c.equals(null));
    }

    @Test
    public void testHasCode() {
        NuxeoPrincipalImpl a = new NuxeoPrincipalImpl("foo");
        NuxeoPrincipalImpl b = new NuxeoPrincipalImpl("foo");
        assertEquals(a.hashCode(), b.hashCode());
        // we don't assert that hash codes are different for principals with
        // different names, as that doesn't have to be true
    }

    @Test
    public void testCopyConstructorContextData() {
        DocumentModel userModel = BaseSession.createEntryModel(null, "user", null, null);
        userModel.putContextData("readonly", true);
        NuxeoPrincipalImpl a = new NuxeoPrincipalImpl("foo");
        a.setModel(userModel);

        NuxeoPrincipalImpl b = new NuxeoPrincipalImpl(a);
        Map<String, Serializable> aContextData = a.getModel().getContextData();
        Map<String, Serializable> bContextData = b.getModel().getContextData();
        assertEquals(aContextData.size(), bContextData.size());
        assertTrue((boolean) aContextData.get("readonly"));
        assertTrue((boolean) bContextData.get("readonly"));
    }

}

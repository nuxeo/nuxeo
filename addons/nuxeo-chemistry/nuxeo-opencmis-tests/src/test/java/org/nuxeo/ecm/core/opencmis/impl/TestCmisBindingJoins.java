/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Test the high-level session using a local connection.
 * <p>
 * Uses the QueryMaker that does CMISQL -> SQL, which allows JOINs.
 */
public class TestCmisBindingJoins extends TestCmisBinding {

    @Before
    public void checkNotDBS() throws Exception {
        assumeTrue("DBS does not support JOINs", !coreFeature.getStorageConfiguration().isDBS());
    }

    @Override
    protected boolean supportsJoins() {
        return true;
    }

    @Override
    protected boolean supportsProxies() {
        return false;
    }

    // no isTrashed querying support for former trash service - force PropertyTrashService
    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-property-override.xml")
    @Override
    public void testQueryTrashed() throws Exception {
        super.testQueryTrashed();
    }
}

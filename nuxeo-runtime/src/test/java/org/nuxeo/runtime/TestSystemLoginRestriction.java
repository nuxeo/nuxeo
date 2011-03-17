/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.runtime;

import java.io.File;

import junit.framework.TestCase;

import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.SystemLoginRestrictionManager;
import org.nuxeo.runtime.util.SimpleRuntime;

public class TestSystemLoginRestriction extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("org.nuxeo.runtime.testing", "true");
        Environment env = new Environment(new File(System.getProperty("java.io.tmpdir")));
        Environment.setDefault(env);
        Framework.initialize(new SimpleRuntime());
    }

    public void testRestrictions() {
        SystemLoginRestrictionManager srm = new SystemLoginRestrictionManager();
        assertTrue(srm.isRemoteSystemLoginRestricted());

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().put(SystemLoginRestrictionManager.RESTRICT_REMOTE_SYSTEM_LOGIN_PROP, "false");
        assertFalse(srm.isRemoteSystemLoginRestricted());

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().put(SystemLoginRestrictionManager.RESTRICT_REMOTE_SYSTEM_LOGIN_PROP, "true");
        assertTrue(srm.isRemoteSystemLoginRestricted());

        srm = new SystemLoginRestrictionManager();
        assertFalse(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost"));

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().put(SystemLoginRestrictionManager.REMOTE_SYSTEM_LOGIN_TRUSTED_INSTANCES_PROP, "RemoteHost");
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost"));

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().put(SystemLoginRestrictionManager.REMOTE_SYSTEM_LOGIN_TRUSTED_INSTANCES_PROP, "RemoteHost,RemoteHost2");
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost"));
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost2"));
        assertFalse(srm.isRemoveSystemLoginAllowedForInstance(""));

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().put(SystemLoginRestrictionManager.REMOTE_SYSTEM_LOGIN_TRUSTED_INSTANCES_PROP, "RemoteHost,RemoteHost2,");
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost"));
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost2"));
        assertFalse(srm.isRemoveSystemLoginAllowedForInstance(""));
    }

}

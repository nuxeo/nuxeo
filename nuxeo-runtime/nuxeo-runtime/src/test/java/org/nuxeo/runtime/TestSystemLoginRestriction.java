/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.SystemLoginRestrictionManager;
import org.nuxeo.runtime.util.SimpleRuntime;

public class TestSystemLoginRestriction {

    private String oldProperty;

    @Before
    public void setUp() throws Exception {
        oldProperty = System.setProperty(Framework.NUXEO_TESTING_SYSTEM_PROP, "true");
        Environment env = new Environment(new File(System.getProperty("java.io.tmpdir")));
        Environment.setDefault(env);
        env.setServerHome(env.getHome());
        env.init();
        Framework.initialize(new SimpleRuntime());
    }

    @After
    public void tearDown() throws Exception {
        if (oldProperty != null) {
            System.setProperty(Framework.NUXEO_TESTING_SYSTEM_PROP, oldProperty);
        } else {
            System.clearProperty(Framework.NUXEO_TESTING_SYSTEM_PROP);
        }
    }

    @Test
    public void testRestrictions() {
        SystemLoginRestrictionManager srm = new SystemLoginRestrictionManager();
        assertTrue(srm.isRemoteSystemLoginRestricted());

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().setProperty(SystemLoginRestrictionManager.RESTRICT_REMOTE_SYSTEM_LOGIN_PROP, "false");
        assertFalse(srm.isRemoteSystemLoginRestricted());

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().setProperty(SystemLoginRestrictionManager.RESTRICT_REMOTE_SYSTEM_LOGIN_PROP, "true");
        assertTrue(srm.isRemoteSystemLoginRestricted());

        srm = new SystemLoginRestrictionManager();
        assertFalse(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost"));

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().setProperty(SystemLoginRestrictionManager.REMOTE_SYSTEM_LOGIN_TRUSTED_INSTANCES_PROP,
                "RemoteHost");
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost"));

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().setProperty(SystemLoginRestrictionManager.REMOTE_SYSTEM_LOGIN_TRUSTED_INSTANCES_PROP,
                "RemoteHost,RemoteHost2");
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost"));
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost2"));
        assertFalse(srm.isRemoveSystemLoginAllowedForInstance(""));

        srm = new SystemLoginRestrictionManager();
        Framework.getProperties().setProperty(SystemLoginRestrictionManager.REMOTE_SYSTEM_LOGIN_TRUSTED_INSTANCES_PROP,
                "RemoteHost,RemoteHost2,");
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost"));
        assertTrue(srm.isRemoveSystemLoginAllowedForInstance("RemoteHost2"));
        assertFalse(srm.isRemoveSystemLoginAllowedForInstance(""));
    }

}

/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.SystemLoginRestrictionManager;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSystemLoginRestriction extends NXRuntimeTestCase{


    @Test
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

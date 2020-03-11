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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.security.Principal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.login.LoginComponent;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestLoginComponentSystemID {

    @Test
    public void testSystemIDEquals() {
        Principal user1 = new LoginComponent.SystemID(null);
        Principal user2 = new LoginComponent.SystemID(null);
        assertNotNull(user1);
        assertEquals(user1, user2);

        Principal otherUser = new LoginComponent.SystemID("toto");
        assertFalse(user1.equals(otherUser));

        Principal nullUser = new LoginComponent.SystemID();
        assertFalse(user1.equals(nullUser));
    }

}

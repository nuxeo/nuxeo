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
 *     Florent Guillaume
 *     Bogdan Stefanescu
 */

package org.nuxeo.ecm.core.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * This test probably should be in org.nuxeo.ecm.core.event.test.EventListenerTest but this would create dependency
 * loops.
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class TestEventContext extends NXRuntimeTestCase {

    @Test
    public void testEventContext() {
        EventContextImpl ctx = new EventContextImpl("arg1", "arg2");
        assertEquals("arg1", ctx.getArguments()[0]);
        assertEquals("arg2", ctx.getArguments()[1]);
        ctx.setProperty("p1", "v1");
        assertEquals("v1", ctx.getProperty("p1"));
        assertEquals(1, ctx.getProperties().size());
        ctx.getProperties().clear();
        assertEquals(0, ctx.getProperties().size());
        assertNull(ctx.getPrincipal());
    }

}
